/**
 * API Client for interacting with Spring Boot backend endpoints
 */
const API = {
    baseUrl: '/api',

    _cache: new Map(),
    _inFlight: new Map(),
    CACHE_TTL_MS: 30000,

    clearCache(prefix) {
        if (!prefix) {
            this._cache.clear();
            return;
        }
        for (const key of this._cache.keys()) {
            if (key.startsWith(prefix)) {
                this._cache.delete(key);
            }
        }
    },

    async get(endpoint, forceRefresh = false) {
        const now = Date.now();
        if (!forceRefresh && this._cache.has(endpoint)) {
            const entry = this._cache.get(endpoint);
            if (now - entry.timestamp < this.CACHE_TTL_MS) {
                return entry.data;
            }
            this._cache.delete(endpoint);
        }

        // Deduplicate simultaneous in-flight requests for the exact same endpoint
        if (this._inFlight.has(endpoint)) {
            return this._inFlight.get(endpoint);
        }

        const fetchPromise = (async () => {
            try {
                const res = await fetch(`${this.baseUrl}${endpoint}`);
                if (!res.ok) {
                    throw new Error(`HTTP Error ${res.status}: ${res.statusText}`);
                }
                const json = await res.json();
                this._cache.set(endpoint, { data: json.data, timestamp: Date.now() });
                return json.data;
            } catch (err) {
                console.error(`API GET error on ${endpoint}:`, err);
                throw err;
            } finally {
                this._inFlight.delete(endpoint);
            }
        })();

        this._inFlight.set(endpoint, fetchPromise);
        return fetchPromise;
    },

    async post(endpoint, body) {
        if (endpoint.includes('/lessons/') || endpoint.includes('/courses/') || endpoint.includes('/quiz/') || endpoint.includes('/discussions')) {
            this.clearCache('/courses/enrolled');
            this.clearCache('/analytics');
            this.clearCache('/users');
            this.clearCache('/discussions');
            this.clearCache('/notifications/recent-emails');
        } else if (endpoint.includes('/notifications')) {
            this.clearCache('/notifications/recent-emails');
            this.clearCache('/notifications/status');
        } else {
            this.clearCache(endpoint);
        }
        try {
            const res = await fetch(`${this.baseUrl}${endpoint}`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: body ? JSON.stringify(body) : null
            });
            const json = await res.json();
            if (!res.ok) {
                throw new Error(json.message || `HTTP Error ${res.status}: ${res.statusText}`);
            }
            return json.data;
        } catch (err) {
            console.error(`API POST error on ${endpoint}:`, err);
            throw err;
        }
    },

    async put(endpoint, body) {
        this.clearCache();
        try {
            const res = await fetch(`${this.baseUrl}${endpoint}`, {
                method: 'PUT',
                headers: { 'Content-Type': 'application/json' },
                body: body ? JSON.stringify(body) : null
            });
            if (!res.ok) {
                throw new Error(`HTTP Error ${res.status}: ${res.statusText}`);
            }
            const json = await res.json();
            return json.data;
        } catch (err) {
            console.error(`API PUT error on ${endpoint}:`, err);
            throw err;
        }
    },

    async delete(endpoint) {
        this.clearCache();
        try {
            const res = await fetch(`${this.baseUrl}${endpoint}`, {
                method: 'DELETE'
            });
            if (!res.ok) {
                throw new Error(`HTTP Error ${res.status}: ${res.statusText}`);
            }
            const json = await res.json();
            return json.data;
        } catch (err) {
            console.error(`API DELETE error on ${endpoint}:`, err);
            throw err;
        }
    },

    // Public Config & Feature Flags
    async getPublicConfig() {
        return this.get('/config');
    },

    // Authentication Endpoints
    async login(email, password) {
        return this.post('/auth/login', { email, password });
    },

    async register(data) {
        return this.post('/auth/register', data);
    },

    async getCurrentUser(userId) {
        return this.get(`/auth/me?userId=${userId}`);
    },

    // Analytics Endpoints
    async getDashboardOverview(userId = 'user_1') {
        return this.get(`/analytics/user/${userId}/dashboard`);
    },

    async getWeeklyActivity(userId = 'user_1') {
        return this.get(`/analytics/user/${userId}/weekly`);
    },

    async getTeamAnalytics() {
        return this.get('/analytics/team/overview');
    },

    // Leaderboard Endpoint
    async getLeaderboard(timeframe = 'WEEKLY', userId = 'user_1', forceRefresh = false) {
        const query = `?timeframe=${encodeURIComponent(timeframe)}&userId=${encodeURIComponent(userId)}`;
        return this.get(`/analytics/leaderboard${query}`, forceRefresh);
    },

    // Course Endpoints
    async getCourses() {
        return this.get('/courses');
    },

    async getCourse(id) {
        return this.get(`/courses/${id}`);
    },

    async createCourse(planDto) {
        return this.post('/courses', planDto);
    },

    async updateCourse(id, planDto) {
        return this.put(`/courses/${id}`, planDto);
    },

    async deleteCourse(id, userId) {
        const query = userId ? `?userId=${encodeURIComponent(userId)}` : '';
        return this.delete(`/courses/${id}${query}`);
    },

    async toggleLesson(courseId, lessonId, userId = 'user_1') {
        return this.post(`/courses/${courseId}/lessons/${lessonId}/toggle?userId=${userId}`);
    },

    async getEnrollment(courseId, userId = 'user_1') {
        return this.get(`/courses/${courseId}/enrollment/${userId}`);
    },

    // User & System
    async getUsers() {
        return this.get('/users');
    },

    async getUser(id, forceRefresh = false) {
        return this.get(`/users/${id}`, forceRefresh);
    },

    async getBadges() {
        return this.get('/users/badges');
    },

    async addUserSkill(userId, skillData) {
        return this.post(`/users/${userId}/skills`, skillData);
    },

    async removeUserSkill(userId, skillId) {
        return this.delete(`/users/${userId}/skills/${skillId}`);
    },

    async updateUserAvatar(userId, avatarUrl) {
        return this.put(`/users/${userId}/avatar`, { avatar: avatarUrl });
    },

    async updateUserTargetRole(userId, roleId) {
        return this.put(`/users/${userId}/target-role/${roleId}`);
    },

    async getRoles() {
        return this.get('/roles');
    },

    async getSkills() {
        return this.get('/skills');
    },

    async getSystemStats() {
        return this.get('/system/stats');
    },

    // Enrolled Courses (My Learning)
    async getEnrolledCourses(userId) {
        return this.get(`/courses/enrolled/${userId}`);
    },

    // AI Plan Generation
    async generateAiPlan(prompt) {
        return this.post('/ai/generate-plan', { prompt });
    },

    // Google Auth
    async googleAuth(name, email, avatar) {
        return this.post('/auth/google', { name, email, avatar });
    },

    // Quiz Arena Endpoints
    async generateQuiz(payload) {
        return this.post('/quiz/generate', payload);
    },

    async getDailyQuiz(userId) {
        return this.get(`/quiz/daily?userId=${encodeURIComponent(userId || 'user_1')}`, true);
    },

    async submitQuiz(payload) {
        return this.post('/quiz/submit', payload);
    },

    // Course Discussion Forum Endpoints
    async getDiscussions({ courseId = '', status = 'ALL', search = '', sort = 'NEWEST', userId = 'user_1' } = {}, forceRefresh = false) {
        const params = new URLSearchParams();
        if (courseId) params.append('courseId', courseId);
        if (status) params.append('status', status);
        if (search) params.append('search', search);
        if (sort) params.append('sort', sort);
        if (userId) params.append('userId', userId);
        return this.get(`/discussions?${params.toString()}`, forceRefresh);
    },

    async getDiscussionDetail(threadId, userId = 'user_1', forceRefresh = false) {
        return this.get(`/discussions/${encodeURIComponent(threadId)}?userId=${encodeURIComponent(userId)}`, forceRefresh);
    },

    async createDiscussionThread(payload) {
        return this.post('/discussions', payload);
    },

    async addDiscussionReply(threadId, payload) {
        return this.post(`/discussions/${encodeURIComponent(threadId)}/replies`, payload);
    },

    async requestAiDiscussionAnswer(threadId) {
        return this.post(`/discussions/${encodeURIComponent(threadId)}/ai-answer`, {});
    },

    async toggleDiscussionThreadUpvote(threadId, userId = 'user_1') {
        return this.post(`/discussions/${encodeURIComponent(threadId)}/upvote?userId=${encodeURIComponent(userId)}`, {});
    },

    async toggleDiscussionReplyUpvote(threadId, replyId, userId = 'user_1') {
        return this.post(`/discussions/${encodeURIComponent(threadId)}/replies/${encodeURIComponent(replyId)}/upvote?userId=${encodeURIComponent(userId)}`, {});
    },

    async resolveDiscussionThread(threadId, acceptedReplyId, userId = 'user_1') {
        return this.post(`/discussions/${encodeURIComponent(threadId)}/resolve`, {
            acceptedReplyId,
            resolvedByUserId: userId
        });
    },

    // =========================================================================
    // ENROLLMENT & ACTIVITY COMPLETION API
    // =========================================================================
    async enrollCourse(courseId, userId = 'user_1') {
        return this.post(`/courses/${encodeURIComponent(courseId)}/enroll?userId=${encodeURIComponent(userId)}`);
    },

    async recordLessonActivity(courseId, lessonId, userId = 'user_1', activityType = 'READING', progressPercent = 100, timeSpentSeconds = 20) {
        return this.post(`/courses/${encodeURIComponent(courseId)}/lessons/${encodeURIComponent(lessonId)}/activity-complete?userId=${encodeURIComponent(userId)}&activityType=${encodeURIComponent(activityType)}&progressPercent=${progressPercent}&timeSpentSeconds=${timeSpentSeconds}`);
    },

    // =========================================================================
    // RESEND EMAIL NOTIFICATIONS API
    // =========================================================================
    async getRecentEmails() {
        this.clearCache('/notifications/recent-emails');
        return this.get('/notifications/recent-emails');
    },

    async sendTestEmail(to = 'delivered@resend.dev', subject = 'Test Notification from CareerPulse') {
        return this.post(`/notifications/send-test?to=${encodeURIComponent(to)}&subject=${encodeURIComponent(subject)}`);
    },

    async getNotificationStatus() {
        return this.get('/notifications/status');
    },

    async configureResendApiKey(apiKey) {
        return this.post('/notifications/configure-key', { apiKey });
    },

    async logActiveStudyTime(userId = 'user_1', minutes = 1) {
        return this.post(`/courses/study-time/log?userId=${encodeURIComponent(userId)}&minutes=${minutes}`);
    }
};

window.API = API;
