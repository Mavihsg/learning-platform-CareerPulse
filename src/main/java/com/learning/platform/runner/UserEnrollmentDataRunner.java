package com.learning.platform.runner;

import com.learning.platform.model.Enrollment;
import com.learning.platform.model.StudyLog;
import com.learning.platform.repository.EnrollmentRepository;
import com.learning.platform.repository.StudyLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;

@Component
@Order(20)
public class UserEnrollmentDataRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(UserEnrollmentDataRunner.class);

    private final EnrollmentRepository enrollmentRepository;
    private final StudyLogRepository studyLogRepository;

    public UserEnrollmentDataRunner(EnrollmentRepository enrollmentRepository,
                                    StudyLogRepository studyLogRepository) {
        this.enrollmentRepository = enrollmentRepository;
        this.studyLogRepository = studyLogRepository;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        LocalDate now = LocalDate.now();
        LocalDate monday = now.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));

        seedUser1IfMissing(monday);
        seedUser2IfMissing(monday);
        seedUser3IfMissing(monday);
        seedUser4IfMissing(monday);
        seedUser5IfMissing(monday);
    }

    private void seedUser1IfMissing(LocalDate monday) {
        if (enrollmentRepository.findByUserId("user_1").isEmpty()) {
            Enrollment core = new Enrollment("user_1", "PLAN_ADE_01");
            core.setIsCoreTrack(true);
            core.setProgressPercentage(38);
            core.setTotalLessons(13);
            core.setCompletedLessonsCount(5);
            core.setRemainingHours(6.0);
            core.setTargetDate("12 Nov 2026");
            core.setCompletedLessonIds(new ArrayList<>(List.of("LES_ADE_01", "LES_ADE_02", "LES_ADE_03", "LES_ADE_04", "LES_ADE_05")));
            core.setCompletedModuleIds(new ArrayList<>(List.of("MOD_ADE_01")));
            enrollmentRepository.save(core);

            Enrollment ae = new Enrollment("user_1", "PLAN_AE_01");
            ae.setIsCoreTrack(false);
            ae.setProgressPercentage(72);
            enrollmentRepository.save(ae);

            Enrollment tw = new Enrollment("user_1", "PLAN_TW_01");
            tw.setIsCoreTrack(false);
            tw.setProgressPercentage(100);
            tw.setStatus(Enrollment.Status.COMPLETED);
            tw.setTotalLessons(2);
            tw.setCompletedLessonsCount(2);
            tw.setRemainingHours(0.0);
            tw.setCompletedAt(LocalDateTime.now().minusDays(2).withHour(14).withMinute(30));
            tw.setCompletedLessonIds(new ArrayList<>(List.of("LES_TW_01", "LES_TW_02")));
            enrollmentRepository.save(tw);

            Enrollment k8s = new Enrollment("user_1", "PLAN_K8S_01");
            k8s.setIsCoreTrack(false);
            k8s.setProgressPercentage(8);
            enrollmentRepository.save(k8s);
            log.info("Seeded enrollments for user_1");
        }

        if (studyLogRepository.findByUserIdAndLogDateBetween("user_1", monday, monday.plusDays(6)).isEmpty()) {
            studyLogRepository.save(new StudyLog("user_1", "M", 35, monday));
            studyLogRepository.save(new StudyLog("user_1", "T", 0, monday.plusDays(1)));
            studyLogRepository.save(new StudyLog("user_1", "W", 55, monday.plusDays(2)));
            studyLogRepository.save(new StudyLog("user_1", "TH", 30, monday.plusDays(3)));
            studyLogRepository.save(new StudyLog("user_1", "F", 85, monday.plusDays(4)));
            studyLogRepository.save(new StudyLog("user_1", "SA", 0, monday.plusDays(5)));
            studyLogRepository.save(new StudyLog("user_1", "SU", 45, monday.plusDays(6)));
            log.info("Seeded current week study logs for user_1 (250m, target met Fri)");
        }
    }

    private void seedUser2IfMissing(LocalDate monday) {
        if (enrollmentRepository.findByUserId("user_2").isEmpty()) {
            Enrollment core = new Enrollment("user_2", "COURSE_SPRING_CLOUD_01");
            core.setIsCoreTrack(true);
            core.setProgressPercentage(85);
            core.setTotalLessons(13);
            core.setCompletedLessonsCount(11);
            core.setRemainingHours(2.0);
            core.setTargetDate("28 Oct 2026");
            core.setCompletedLessonIds(new ArrayList<>(List.of("LES_SC_01", "LES_SC_02")));
            core.setCompletedModuleIds(new ArrayList<>(List.of("MOD_SC_01")));
            enrollmentRepository.save(core);

            Enrollment tw = new Enrollment("user_2", "PLAN_TW_01");
            tw.setIsCoreTrack(false);
            tw.setProgressPercentage(100);
            tw.setStatus(Enrollment.Status.COMPLETED);
            tw.setTotalLessons(2);
            tw.setCompletedLessonsCount(2);
            tw.setRemainingHours(0.0);
            tw.setCompletedAt(LocalDateTime.now().minusDays(1).withHour(11).withMinute(15));
            tw.setCompletedLessonIds(new ArrayList<>(List.of("LES_TW_01", "LES_TW_02")));
            enrollmentRepository.save(tw);

            Enrollment ae = new Enrollment("user_2", "PLAN_AE_01");
            ae.setIsCoreTrack(false);
            ae.setProgressPercentage(40);
            ae.setStatus(Enrollment.Status.IN_PROGRESS);
            ae.setTotalLessons(2);
            ae.setCompletedLessonsCount(1);
            ae.setRemainingHours(1.2);
            ae.setCompletedLessonIds(new ArrayList<>(List.of("LES_AE_01")));
            enrollmentRepository.save(ae);

            log.info("Seeded enrollments for user_2 (Isha Agarwal)");
        }

        if (studyLogRepository.findByUserIdAndLogDateBetween("user_2", monday, monday.plusDays(6)).isEmpty()) {
            studyLogRepository.save(new StudyLog("user_2", "M", 55, monday));
            studyLogRepository.save(new StudyLog("user_2", "T", 60, monday.plusDays(1)));
            studyLogRepository.save(new StudyLog("user_2", "W", 65, monday.plusDays(2)));
            studyLogRepository.save(new StudyLog("user_2", "TH", 70, monday.plusDays(3))); // 55+60+65+70 = 250m on Thursday!
            studyLogRepository.save(new StudyLog("user_2", "F", 35, monday.plusDays(4)));
            studyLogRepository.save(new StudyLog("user_2", "SA", 0, monday.plusDays(5)));
            studyLogRepository.save(new StudyLog("user_2", "SU", 0, monday.plusDays(6)));
            log.info("Seeded current week study logs for user_2 (285m, target met Thu)");
        }
    }

    private void seedUser3IfMissing(LocalDate monday) {
        if (enrollmentRepository.findByUserId("user_3").isEmpty()) {
            Enrollment core = new Enrollment("user_3", "PLAN_K8S_01");
            core.setIsCoreTrack(true);
            core.setProgressPercentage(20);
            core.setTotalLessons(2);
            core.setCompletedLessonsCount(0);
            core.setRemainingHours(3.5);
            core.setTargetDate("15 Dec 2026");
            enrollmentRepository.save(core);

            Enrollment ade = new Enrollment("user_3", "PLAN_ADE_01");
            ade.setIsCoreTrack(false);
            ade.setProgressPercentage(15);
            ade.setTotalLessons(13);
            ade.setCompletedLessonsCount(2);
            ade.setRemainingHours(8.5);
            enrollmentRepository.save(ade);
            log.info("Seeded enrollments for user_3 (Pulkit Jain)");
        }

        if (studyLogRepository.findByUserIdAndLogDateBetween("user_3", monday, monday.plusDays(6)).isEmpty()) {
            studyLogRepository.save(new StudyLog("user_3", "M", 25, monday));
            studyLogRepository.save(new StudyLog("user_3", "T", 35, monday.plusDays(1)));
            studyLogRepository.save(new StudyLog("user_3", "W", 0, monday.plusDays(2)));
            studyLogRepository.save(new StudyLog("user_3", "TH", 0, monday.plusDays(3)));
            studyLogRepository.save(new StudyLog("user_3", "F", 40, monday.plusDays(4)));
            studyLogRepository.save(new StudyLog("user_3", "SA", 0, monday.plusDays(5)));
            studyLogRepository.save(new StudyLog("user_3", "SU", 0, monday.plusDays(6)));
            log.info("Seeded current week study logs for user_3 (100m, target in progress)");
        }
    }

    private void seedUser4IfMissing(LocalDate monday) {
        if (enrollmentRepository.findByUserId("user_4").isEmpty()) {
            Enrollment core = new Enrollment("user_4", "COURSE_SPRING_CLOUD_01");
            core.setIsCoreTrack(true);
            core.setProgressPercentage(100);
            core.setStatus(Enrollment.Status.COMPLETED);
            core.setTotalLessons(2);
            core.setCompletedLessonsCount(2);
            core.setRemainingHours(0.0);
            core.setCompletedLessonIds(new ArrayList<>(List.of("LES_SC_01", "LES_SC_02")));
            enrollmentRepository.save(core);

            Enrollment ade = new Enrollment("user_4", "PLAN_ADE_01");
            ade.setIsCoreTrack(false);
            ade.setProgressPercentage(100);
            ade.setStatus(Enrollment.Status.COMPLETED);
            ade.setTotalLessons(13);
            ade.setCompletedLessonsCount(13);
            ade.setRemainingHours(0.0);
            enrollmentRepository.save(ade);

            Enrollment tw = new Enrollment("user_4", "PLAN_TW_01");
            tw.setIsCoreTrack(false);
            tw.setProgressPercentage(100);
            tw.setStatus(Enrollment.Status.COMPLETED);
            tw.setTotalLessons(2);
            tw.setCompletedLessonsCount(2);
            tw.setRemainingHours(0.0);
            enrollmentRepository.save(tw);

            log.info("Seeded enrollments for user_4 (Nikhil Khanna)");
        }

        if (studyLogRepository.findByUserIdAndLogDateBetween("user_4", monday, monday.plusDays(6)).isEmpty()) {
            studyLogRepository.save(new StudyLog("user_4", "M", 60, monday));
            studyLogRepository.save(new StudyLog("user_4", "T", 75, monday.plusDays(1)));
            studyLogRepository.save(new StudyLog("user_4", "W", 65, monday.plusDays(2))); // 60+75+65 = 200m
            studyLogRepository.save(new StudyLog("user_4", "TH", 50, monday.plusDays(3))); // 250m on Thursday!
            studyLogRepository.save(new StudyLog("user_4", "F", 60, monday.plusDays(4)));
            studyLogRepository.save(new StudyLog("user_4", "SA", 50, monday.plusDays(5)));
            studyLogRepository.save(new StudyLog("user_4", "SU", 40, monday.plusDays(6)));
            log.info("Seeded current week study logs for user_4 (400m)");
        }
    }

    private void seedUser5IfMissing(LocalDate monday) {
        if (enrollmentRepository.findByUserId("user_5").isEmpty()) {
            Enrollment core = new Enrollment("user_5", "COURSE_SPRING_CLOUD_01");
            core.setIsCoreTrack(true);
            core.setProgressPercentage(60);
            core.setTotalLessons(2);
            core.setCompletedLessonsCount(1);
            core.setRemainingHours(2.5);
            core.setCompletedLessonIds(new ArrayList<>(List.of("LES_SC_01")));
            enrollmentRepository.save(core);

            Enrollment tw = new Enrollment("user_5", "PLAN_TW_01");
            tw.setIsCoreTrack(false);
            tw.setProgressPercentage(100);
            tw.setStatus(Enrollment.Status.COMPLETED);
            tw.setTotalLessons(2);
            tw.setCompletedLessonsCount(2);
            tw.setRemainingHours(0.0);
            enrollmentRepository.save(tw);

            log.info("Seeded enrollments for user_5 (Rhea Sharma)");
        }

        if (studyLogRepository.findByUserIdAndLogDateBetween("user_5", monday, monday.plusDays(6)).isEmpty()) {
            studyLogRepository.save(new StudyLog("user_5", "M", 40, monday));
            studyLogRepository.save(new StudyLog("user_5", "T", 35, monday.plusDays(1)));
            studyLogRepository.save(new StudyLog("user_5", "W", 45, monday.plusDays(2)));
            studyLogRepository.save(new StudyLog("user_5", "TH", 40, monday.plusDays(3)));
            studyLogRepository.save(new StudyLog("user_5", "F", 50, monday.plusDays(4)));
            studyLogRepository.save(new StudyLog("user_5", "SA", 30, monday.plusDays(5)));
            studyLogRepository.save(new StudyLog("user_5", "SU", 0, monday.plusDays(6)));
            log.info("Seeded current week study logs for user_5 (240m, target met Fri)");
        }
    }
}
