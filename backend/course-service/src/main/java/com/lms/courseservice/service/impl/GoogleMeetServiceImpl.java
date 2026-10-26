package com.lms.courseservice.service.impl;

import com.lms.courseservice.dto.MeetingInfo;
import com.lms.courseservice.service.GoogleMeetService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class GoogleMeetServiceImpl implements GoogleMeetService {

    @Override
    public MeetingInfo createMeeting(String title) {
        String uid = UUID.randomUUID().toString().replace("-", "");
        String meetUrl = "https://meet.google.com/lms-" + uid.substring(0, 4) + "-" + uid.substring(4, 8);
        return MeetingInfo.builder()
                .meetingUrl(meetUrl)
                .eventId("event-" + uid.substring(0, 8))
                .build();
    }

    @Override
    public void deleteMeeting(String eventId) {
        log.info("Deleting meeting event: {}", eventId);
    }
}
