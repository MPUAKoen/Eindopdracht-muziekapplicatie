package com.example.demo.dto;

public class PracticeSummaryDto {

    public static class RangeSummary {
        private long today;
        private long last7Days;
        private long last30Days;
        private long last365Days;
        private long allTime;

        public long getToday() {
            return today;
        }

        public void setToday(long today) {
            this.today = today;
        }

        public long getLast7Days() {
            return last7Days;
        }

        public void setLast7Days(long last7Days) {
            this.last7Days = last7Days;
        }

        public long getLast30Days() {
            return last30Days;
        }

        public void setLast30Days(long last30Days) {
            this.last30Days = last30Days;
        }

        public long getLast365Days() {
            return last365Days;
        }

        public void setLast365Days(long last365Days) {
            this.last365Days = last365Days;
        }

        public long getAllTime() {
            return allTime;
        }

        public void setAllTime(long allTime) {
            this.allTime = allTime;
        }
    }

    private RangeSummary practiceHours;
    private RangeSummary guidedLessonHours;
    private RangeSummary listenedHours;

    public RangeSummary getPracticeHours() {
        return practiceHours;
    }

    public void setPracticeHours(RangeSummary practiceHours) {
        this.practiceHours = practiceHours;
    }

    public RangeSummary getGuidedLessonHours() {
        return guidedLessonHours;
    }

    public void setGuidedLessonHours(RangeSummary guidedLessonHours) {
        this.guidedLessonHours = guidedLessonHours;
    }

    public RangeSummary getListenedHours() {
        return listenedHours;
    }

    public void setListenedHours(RangeSummary listenedHours) {
        this.listenedHours = listenedHours;
    }
}
