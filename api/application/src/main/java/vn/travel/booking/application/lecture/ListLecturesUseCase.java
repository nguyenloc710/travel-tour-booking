package vn.travel.booking.application.lecture;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.travel.booking.application.market.Markets;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

@Service
public class ListLecturesUseCase {

    private final LectureQueryPort lectures;
    private final Markets markets;
    private final Clock clock;

    public ListLecturesUseCase(LectureQueryPort lectures, Markets markets, Clock clock) {
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
