package com.chatapp.chatservice.service;

import com.chatapp.chatservice.entity.Poll;
import com.chatapp.chatservice.entity.PollOption;
import com.chatapp.chatservice.repository.PollRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PollService {

    private final PollRepository pollRepository;

    @Transactional
    public Poll createPoll(Poll poll, List<PollOption> options) {
        poll.setCreatedAt(LocalDateTime.now());
        poll.setActive(true);
        
        // 🛑 NAYA FIX: Save karne se pehle hi Options ko unka parent (Poll) assign kar do
        if (options != null) {
            for (PollOption option : options) {
                option.setPoll(poll); // Hibernate ko pata chal jayega ki iska poll_id kya hai
            }
            poll.setOptions(options);
        }

        // Ab database mein Poll aur uske saare Options ek hi command mein successfully save ho jayenge
        return pollRepository.save(poll);
    }
}