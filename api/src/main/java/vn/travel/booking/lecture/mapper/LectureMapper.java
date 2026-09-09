package vn.travel.booking.lecture.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import vn.travel.booking.lecture.dto.LectureSummary;
import vn.travel.booking.web.generated.model.Lecture;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

@Mapper(componentModel = "spring")
public interface LectureMapper {

    DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    @Mapping(target = "startTime", expression = "java(formatTime(l.startTime()))")
    Lecture toView(LectureSummary l);

    default String formatTime(LocalTime time) {
        return time == null ? null : time.format(TIME_FORMATTER);
    }
}
