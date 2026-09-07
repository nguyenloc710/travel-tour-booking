package vn.travel.booking.lecture.service;

import vn.travel.booking.lecture.dto.LectureSummary;
import vn.travel.booking.lecture.repository.LectureRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.travel.booking.market.service.MarketService;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

@Service
public class LectureService {

    private final LectureRepository lectures;
    private final MarketService markets;
    private final Clock clock;

    public LectureService(LectureRepository lectures, MarketService markets, Clock clock) {
        this.lectures = lectures;
        this.markets = markets;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<LectureSummary> execute(String market, String locale) {
        markets.requireActive(market);
        return lectures.findUpcoming(market, locale, LocalDate.now(clock));
    }
}
