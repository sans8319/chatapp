package com.chatapp.chatservice.service;

import com.chatapp.chatservice.entity.*;
import com.chatapp.chatservice.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PollService {

    private final PollRepository pollRepository;
    private final UserRepository userRepository;
    private final PollResponseRepository responseRepository;
    private final SimpMessagingTemplate messagingTemplate; 

    @Transactional
    public Poll createPoll(Poll poll, List<PollOption> options) {
        poll.setCreatedAt(LocalDateTime.now());
        poll.setActive(true);
        if (options != null) {
            for (PollOption option : options) { option.setPoll(poll); }
            poll.setOptions(options);
        }
        return pollRepository.save(poll);
    }

    public List<Poll> getPollsForUser(Long userId) {
        User user = userRepository.findById(userId).orElseThrow();
        String dept = user.getDepartment() != null ? user.getDepartment() : "";
        List<Poll> polls = pollRepository.findVisiblePolls(dept, userId);

        polls.forEach(p -> {
            userRepository.findById(p.getCreatedBy()).ifPresent(u -> p.setCreatedByUsername(u.getUsername()));
            Optional<PollResponse> userResponseOpt = responseRepository.findByPollIdAndUserId(p.getId(), userId);
            p.setUserVoted(userResponseOpt.isPresent());

            boolean targeted = p.getTargetedAudience().equals("all") ||
                    (p.getTargetedAudience().equals("department") && p.getTargetDepartments().contains(dept)) ||
                    (p.getTargetedAudience().equals("specific") && p.getTargetUsers().contains(userId));
            p.setTargetedForUser(targeted);

            List<Long> userSelectedOptions = userResponseOpt.isPresent() ? userResponseOpt.get().getSelectedOptionIds() : java.util.Collections.emptyList();

            if (p.getOptions() != null) {
                p.getOptions().forEach(opt -> {
                    opt.setVoteCount(responseRepository.countVotesByOptionId(opt.getId()));
                    opt.setUserChoice(userSelectedOptions.contains(opt.getId()));
                });
            }

            // 🛑 CLEAN FIX: Yahan saare votes nikal kar Poll me attach kar diye
            List<PollResponse> allResponses = responseRepository.findAll().stream()
                    .filter(r -> r.getPoll().getId().equals(p.getId()))
                    .collect(Collectors.toList());
            
            allResponses.forEach(r -> {
                userRepository.findById(r.getUserId()).ifPresent(r::setUser);
            });
            p.setVotes(allResponses);
        });

        return polls;
    }

    public PollResponse saveVote(PollResponse response) {
        if (responseRepository.existsByPollIdAndUserId(response.getPoll().getId(), response.getUserId())) {
            throw new RuntimeException("You have already voted in this poll!");
        }
        PollResponse saved = responseRepository.save(response);
        messagingTemplate.convertAndSend("/topic/public/updates", Map.of("type", "VOTE_SUBMITTED"));
        return saved;
    }
}