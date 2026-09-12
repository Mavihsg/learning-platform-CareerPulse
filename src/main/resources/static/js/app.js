/**
 * Master Application Controller - Career Pulse
 */
document.addEventListener('DOMContentLoaded', () => {
    App.init();
});

const App = {
    currentUserId: 'user_1',
    currentUser: null,
    dashboardData: null,
    courses: [],
    currentPlanId: 'PLAN_ADE_01',
    activeCourse: null,
    activeEnrollment: null,
    activeModalLesson: null,
    teamData: null,
    myLearningCourses: [],
    myLearningActiveFilter: 'all',
    confettiAnimationId: null,

    // Quiz Arena state
    quizState: {
        activeQuiz: null,
        currentQuestionIndex: 0,
        userAnswers: {},
        isDaily: false,
        selectedCourseIds: [],
        availableCompletedCourses: []
    },

    // Course Discussions Forum state
    discussionState: {
        threads: [],
        activeThread: null,
        selectedCourseId: '',
        selectedStatus: 'ALL',
        searchQuery: '',
        selectedSort: 'NEWEST',
        isLoading: false,
        searchDebounceTimer: null
    },

    // Initial Plan Builder state
    builderState: {
        id: null, // null for new plan, or course ID when editing
        title: 'Platform Engineering Fundamentals',
        track: 'Data',
        category: 'Engineering',
        difficultyLevel: 'INTERMEDIATE',
        modules: [
            {
                title: 'Runtime basics',
                lessons: [
                    { title: 'Processes and isolation', resourceType: 'VIDEO', durationMinutes: 20, videoUrl: 'https://www.youtube-nocookie.com/embed/1FUcniACzmc', content: '### Process Isolation in Modern Operating Systems\n\nNamespaces and cgroups form the isolation primitives behind Linux containers.' },
                    { title: 'Images and layers', resourceType: 'READING', durationMinutes: 25, content: '### OCI Image Specification & OverlayFS\n\nContainer images are composed of immutable read-only layers with a copy-on-write overlay.' }
                ]
            },
            {
                title: 'Deployment',
                lessons: [
                    { title: 'Rollout strategies', resourceType: 'VIDEO', durationMinutes: 18, videoUrl: 'https://www.youtube-nocookie.com/embed/1FUcniACzmc', content: '### Canary vs Blue-Green Deployments\n\nLearn how traffic splitting minimizes blast radius during software releases.' },
                    { title: 'Exercise: blue-green', resourceType: 'EXERCISE', durationMinutes: 45, content: '### Hands-on: Zero-Downtime Blue-Green Switch\n\nConfigure an Ingress resource to route 100% of live traffic to the green deployment.' }
                ]
            }
        ]
    },

    // Runtime App Configuration & Feature Flags
    appConfig: {
        demoUsersEnabled: true,
        environment: 'development'
    },

    async init() {
        this.initNetflixIntro();
        this.initTheme();
        this.bindEvents();
        await this.loadAppConfig();
        this.checkAuth();
        this.refreshEmailAudits();
    },

    async loadAppConfig() {
        try {
            const config = await API.getPublicConfig();
            if (config) {
                this.appConfig = { ...this.appConfig, ...config };
            }
        } catch (err) {
            console.warn('Could not fetch /api/config, applying fallback detection:', err);
            // If running on non-localhost without config, default to hiding demo accounts
            const isLocal = window.location.hostname === 'localhost' || window.location.hostname === '127.0.0.1';
            this.appConfig.demoUsersEnabled = isLocal;
        }
        this.applyFeatureVisibility();
    },

    applyFeatureVisibility() {
        const demoSection = document.getElementById('quick-demo-section');
        if (demoSection) {
            demoSection.style.display = this.appConfig.demoUsersEnabled ? 'block' : 'none';
        }
    },

    initNetflixIntro() {
        const overlay = document.getElementById('netflix-intro-overlay');
        if (!overlay) return;

        // Auto-dismiss after 2.3 seconds with smooth fade
        this._introTimer = setTimeout(() => {
            this.dismissNetflixIntro();
        }, 2300);
    },

    dismissNetflixIntro() {
        const overlay = document.getElementById('netflix-intro-overlay');
        if (!overlay) return;
        if (this._introTimer) {
            clearTimeout(this._introTimer);
            this._introTimer = null;
        }
        overlay.classList.add('netflix-fade-out');
        setTimeout(() => {
            overlay.style.display = 'none';
        }, 450);
    },

    initTheme() {
        const savedTheme = localStorage.getItem('career_pulse_theme') || 'light';
        this.setTheme(savedTheme);
    },

    setTheme(theme) {
        document.documentElement.setAttribute('data-theme', theme);
        localStorage.setItem('career_pulse_theme', theme);
        const iconSvg = document.getElementById('theme-icon-svg');
        if (iconSvg) {
            if (theme === 'dark') {
                iconSvg.innerHTML = `<circle cx="12" cy="12" r="4"/><path d="M12 2v2"/><path d="M12 20v2"/><path d="m4.93 4.93 1.41 1.41"/><path d="m17.66 17.66 1.41 1.41"/><path d="M2 12h2"/><path d="M20 12h2"/><path d="m6.34 17.66-1.41 1.41"/><path d="m19.07 4.93-1.41 1.41"/>`;
            } else {
                iconSvg.innerHTML = `<path d="M12 3a6 6 0 0 0 9 9 9 9 0 1 1-9-9Z"/>`;
            }
        }
    },

    toggleTheme() {
        const current = document.documentElement.getAttribute('data-theme') || 'light';
        const next = current === 'dark' ? 'light' : 'dark';
        this.setTheme(next);
    },

    bindEvents() {
        const themeBtn = document.getElementById('theme-toggle-btn');
        if (themeBtn) {
            themeBtn.addEventListener('click', () => this.toggleTheme());
        }

        const planTitleInput = document.getElementById('builder-plan-title');
        if (planTitleInput) {
            planTitleInput.addEventListener('input', (e) => {
                this.builderState.title = e.target.value;
            });
        }

        const planTrackInput = document.getElementById('builder-plan-track');
        if (planTrackInput) {
            planTrackInput.addEventListener('input', (e) => {
                this.builderState.track = e.target.value;
            });
        }

        // Close Level/XP Modal on Escape key press
        window.addEventListener('keydown', (e) => {
            if (e.key === 'Escape') {
                this.closeLevelModal();
            }
        });
    },

    // =========================================================================
    // AUTHENTICATION & STARTUP GATE
    // =========================================================================
    checkAuth() {
        const storedUser = localStorage.getItem('career_pulse_auth_user');
        if (storedUser) {
            try {
                this.currentUser = JSON.parse(storedUser);
                this.currentUserId = this.currentUser.id || 'user_1';
                this.hideAuthModal();
                this.loadInitialData();
            } catch (e) {
                this.showAuthModal();
            }
        } else {
            this.showAuthModal();
        }
    },

    showAuthModal() {
        this.applyFeatureVisibility();
        const modal = document.getElementById('auth-modal');
        if (modal) modal.style.display = 'flex';
    },

    hideAuthModal() {
        const modal = document.getElementById('auth-modal');
        if (modal) modal.style.display = 'none';
    },

    switchAuthTab(tab) {
        document.getElementById('tab-btn-signin').classList.toggle('active', tab === 'signin');
        document.getElementById('tab-btn-register').classList.toggle('active', tab === 'register');
        document.getElementById('auth-form-signin').style.display = tab === 'signin' ? 'flex' : 'none';
        document.getElementById('auth-form-register').style.display = tab === 'register' ? 'flex' : 'none';
        document.getElementById('auth-main-title').textContent = tab === 'signin' ? 'Sign in to your account' : 'Create your learner profile';
    },

    async handleLogin(e) {
        if (e) e.preventDefault();
        const email = document.getElementById('auth-email').value;
        const password = document.getElementById('auth-password').value;
        const submitBtn = document.getElementById('btn-submit-signin');
        if (submitBtn) submitBtn.textContent = 'Signing in...';

        try {
            const res = await API.login(email, password);
            if (res && res.user) {
                try { sessionStorage.clear(); } catch (e) {}
                this.dashboardData = null;
                this.myLearningCourses = null;
                this.myLearningUserId = null;
                this.currentUser = res.user;
                this.currentUserId = res.user.id;
                localStorage.setItem('career_pulse_auth_user', JSON.stringify(res.user));
                this.hideAuthModal();
                await this.loadInitialData();
            }
        } catch (err) {
            alert('Authentication failed: ' + err.message);
        } finally {
            if (submitBtn) submitBtn.textContent = 'Sign In to Portal';
        }
    },

    async handleRegister(e) {
        if (e) e.preventDefault();
        const name = document.getElementById('reg-name').value;
        const email = document.getElementById('reg-email').value;
        const role = document.getElementById('reg-role').value;
        const track = document.getElementById('reg-track').value;
        const submitBtn = document.getElementById('btn-submit-reg');
        if (submitBtn) submitBtn.textContent = 'Creating account...';

        try {
            const res = await API.register({ name, email, currentRoleTitle: role, targetRoleId: track });
            if (res && res.user) {
                this.currentUser = res.user;
                this.currentUserId = res.user.id;
                localStorage.setItem('career_pulse_auth_user', JSON.stringify(res.user));
                this.hideAuthModal();
                await this.loadInitialData();
            }
        } catch (err) {
            alert('Registration failed: ' + err.message);
        } finally {
            if (submitBtn) submitBtn.textContent = 'Create Learner Profile';
        }
    },

    async quickLogin(userId) {
        if (this.appConfig && this.appConfig.demoUsersEnabled === false) {
            console.warn('Quick login is disabled in this environment.');
            alert('Quick demo logins are disabled in production.');
            return;
        }
        try {
            try { sessionStorage.clear(); } catch (e) {}
            this.dashboardData = null;
            this.myLearningCourses = null;
            this.myLearningUserId = null;
            this.catalogEnrollmentMap = null;
            const user = await API.getUser(userId);
            if (user) {
                this.currentUser = user;
                this.currentUserId = user.id;
                localStorage.setItem('career_pulse_auth_user', JSON.stringify(user));
                this.hideAuthModal();
                await this.loadInitialData();
            }
        } catch (err) {
            alert('Quick login error: ' + err.message);
        }
    },

    initGoogleSignIn() {
        if (typeof google === 'undefined' || !google.accounts) {
            alert('Google Identity Services script failed to load. Please try again.');
            return;
        }
        
        google.accounts.id.initialize({
            client_id: '516054535351-f3bdp0ra9g91304bnmavf38p6jttomfk.apps.googleusercontent.com',
            callback: this.handleGoogleSignIn.bind(this)
        });
        
        // This triggers the Google One Tap popup
        google.accounts.id.prompt();
    },

    async handleGoogleSignIn(response) {
        try {
            // Decode the JWT token payload
            const base64Url = response.credential.split('.')[1];
            const base64 = base64Url.replace(/-/g, '+').replace(/_/g, '/');
            const jsonPayload = decodeURIComponent(atob(base64).split('').map(function(c) {
                return '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2);
            }).join(''));
            
            const payload = JSON.parse(jsonPayload);
            const res = await API.googleAuth(payload.name, payload.email, payload.picture);
            
            if (res && res.user) {
                this.currentUser = res.user;
                this.currentUserId = res.user.id;
                localStorage.setItem('career_pulse_auth_user', JSON.stringify(res.user));
                this.hideAuthModal();
                await this.loadInitialData();
            }
        } catch (err) {
            console.error('Google Sign-In failed:', err);
            alert('Google Sign-In failed: ' + err.message);
        }
    },

    signOut() {
        localStorage.removeItem('career_pulse_auth_user');
        this.currentUser = null;
        this.currentUserId = null;
        this.dashboardData = null;
        this.courses = [];
        this.activeCourse = null;
        this.activeEnrollment = null;
        this.teamData = null;
        // Full reload to reset all state and show auth gate
        window.location.reload();
    },

    navigate(viewName) {
        document.querySelectorAll('.nav-item').forEach(btn => btn.classList.remove('active'));
        const activeNavBtn = document.getElementById(`nav-${viewName}`);
        if (activeNavBtn) activeNavBtn.classList.add('active');

        document.querySelectorAll('.view-panel').forEach(panel => panel.classList.remove('active'));
        const activePanel = document.getElementById(`view-${viewName}`);
        if (activePanel) activePanel.classList.add('active');

        const breadcrumbEl = document.getElementById('header-breadcrumb');
        if (breadcrumbEl) {
            breadcrumbEl.textContent = viewName.toUpperCase().replace('-', ' ');
        }

        if (viewName === 'dashboard') {
            this.loadDashboard();
        } else if (viewName === 'catalog') {
            this.loadCatalog();
        } else if (viewName === 'my-learning') {
            this.loadMyLearning();
        } else if (viewName === 'plan') {
            this.loadPlanOverview(this.currentPlanId);
        } else if (viewName === 'builder') {
            this.renderBuilderView();
        } else if (viewName === 'my-courses') {
            this.loadMyCourses();
        } else if (viewName === 'team') {
            this.loadTeamAnalytics();
        } else if (viewName === 'profile') {
            this.loadProfile();
        } else if (viewName === 'leaderboard') {
            this.loadLeaderboard();
        } else if (viewName === 'quiz-arena') {
            this.loadQuizArena();
        } else if (viewName === 'discussions') {
            this.loadDiscussions();
        }
    },

    async loadInitialData() {
        try {
            await Promise.all([
                this.loadDashboard(),
                this.loadCatalog(),
                this.syncUserGamification(true)
            ]);
            this.renderBuilderView();
            const activePanel = document.querySelector('.view-panel.active');
            if (activePanel) {
                if (activePanel.id === 'view-my-courses') {
                    await this.loadMyCourses();
                } else if (activePanel.id === 'view-my-learning') {
                    await this.loadMyLearning();
                } else if (activePanel.id === 'view-profile') {
                    await this.loadProfile();
                } else if (activePanel.id === 'view-leaderboard') {
                    await this.loadLeaderboard();
                }
            }
        } catch (err) {
            console.error('Initial load error:', err);
        }
    },

    // =========================================================================
    // VIEW 1: DASHBOARD
    // =========================================================================
    async loadDashboard() {
        // Fast instant render from session cache
        if (!this.dashboardData || (this.dashboardData.userId && this.dashboardData.userId !== this.currentUserId)) {
            try {
                const cached = sessionStorage.getItem(`cp_dash_${this.currentUserId}`);
                if (cached) {
                    this.dashboardData = JSON.parse(cached);
                    this.renderDashboard(this.dashboardData);
                }
            } catch (e) {}
        }

        try {
            const data = await API.getDashboardOverview(this.currentUserId);
            this.dashboardData = data;
            try { sessionStorage.setItem(`cp_dash_${this.currentUserId}`, JSON.stringify(data)); } catch (e) {}
            this.renderDashboard(data);
        } catch (err) {
            console.error('Failed to load dashboard:', err);
        }
    },

    renderDashboard(d) {
        if (!d) return;

        const el = (id) => document.getElementById(id);

        const greetingEl = el('dash-greeting');
        if (greetingEl) {
            greetingEl.textContent = d.greeting || `Good afternoon, ${d.userName ? d.userName.split(' ')[0] : 'Shivam'}`;
        }
        const subtitleEl = el('dash-subtitle');
        if (subtitleEl) {
            subtitleEl.textContent = d.subtitle || 'Five lessons behind you this week. One module left before the capstone.';
        }
        const sideNameEl = el('sidebar-user-name');
        if (sideNameEl) {
            sideNameEl.textContent = d.userName || 'Shivam Gupta';
        }

        if (this.currentUser) {
            const sideRoleEl = el('sidebar-user-role');
            if (sideRoleEl) {
                sideRoleEl.textContent = this.currentUser.currentRoleTitle || this.currentUser.role || 'Junior Data Engineer';
            }
            const sideAvatarEl = el('sidebar-avatar');
            if (sideAvatarEl) {
                sideAvatarEl.src = this.currentUser.avatar || this.currentUser.avatarUrl || `https://api.dicebear.com/7.x/bottts/svg?seed=${d.userName || 'Shivam'}`;
            }
        }

        if (d.coreTrackId) {
            this.currentPlanId = d.coreTrackId;
        }

        const pct = d.coreTrackProgressPercentage !== undefined ? d.coreTrackProgressPercentage : 0;
        if (el('dash-core-title')) el('dash-core-title').textContent = d.coreTrackTitle || 'No Active Core Track';
        if (el('dash-core-sub')) el('dash-core-sub').textContent = d.coreTrackSubtitle || 'Explore courses to begin learning';
        if (el('dash-core-pct-badge')) el('dash-core-pct-badge').textContent = `${pct}% complete`;
        if (el('header-core-pct')) el('header-core-pct').textContent = `${pct}%`;
        if (el('dash-donut-text')) el('dash-donut-text').textContent = `${pct}%`;

        const totalCircumference = 251.2;
        const strokeOffset = totalCircumference - (totalCircumference * (pct / 100));
        const donutCircle = el('dash-donut-fill');
        if (donutCircle) {
            donutCircle.style.strokeDashoffset = strokeOffset;
        }

        const doneL = d.coreTrackLessonsDone !== undefined ? d.coreTrackLessonsDone : 0;
        const totalL = d.coreTrackTotalLessons !== undefined ? d.coreTrackTotalLessons : 0;
        if (el('dash-stat-lessons')) el('dash-stat-lessons').textContent = `${doneL}/${totalL}`;
        if (el('dash-stat-time')) el('dash-stat-time').textContent = `${d.timeLoggedHours !== undefined ? d.timeLoggedHours : 0} h`;
        if (el('dash-stat-streak')) el('dash-stat-streak').textContent = `${d.streakDays !== undefined ? d.streakDays : 0}`;
        if (el('dash-stat-plans')) el('dash-stat-plans').textContent = `${d.plansEnrolledCount !== undefined ? d.plansEnrolledCount : 0}`;

        if (el('hud-streak-val')) el('hud-streak-val').textContent = `${d.streakDays !== undefined ? d.streakDays : 0}`;

        const userLevel = d.currentLevel !== undefined ? d.currentLevel : (this.currentUser ? this.currentUser.currentLevel : 1);
        const userLevelTitle = d.levelTitle || (this.currentUser ? this.currentUser.levelTitle : 'Novice Explorer');
        const userXp = d.currentXp !== undefined ? d.currentXp : (this.currentUser ? (this.currentUser.currentXp || 0) : 0);
        const userStreak = d.streakDays !== undefined ? d.streakDays : (this.currentUser ? (this.currentUser.streakDays || 0) : 0);
        const userShields = d.shieldCount !== undefined ? d.shieldCount : (this.currentUser ? (this.currentUser.shieldCount || 0) : 0);

        if (el('dash-hero-streak')) el('dash-hero-streak').textContent = `${userStreak} Days`;
        if (el('dash-hero-xp')) el('dash-hero-xp').textContent = `${userXp.toLocaleString()} XP`;
        if (el('dash-hero-shield')) el('dash-hero-shield').textContent = `${userShields} Shield${userShields !== 1 ? 's' : ''}`;
        if (el('dash-hero-level')) el('dash-hero-level').textContent = `Level ${userLevel}`;
        if (el('dash-hero-level-title')) el('dash-hero-level-title').textContent = userLevelTitle;

        // Synchronize header HUD level pill
        if (el('hud-level-val')) el('hud-level-val').textContent = `LVL ${userLevel} · ${userLevelTitle.toUpperCase()}`;

        // Dynamic Daily Quiz banner streak title
        if (el('dash-daily-quiz-title')) {
            if (userStreak > 0) {
                el('dash-daily-quiz-title').textContent = `Keep your ${userStreak}-day streak alive!`;
            } else {
                el('dash-daily-quiz-title').textContent = `Start your learning streak today!`;
            }
        }

        if (el('dash-core-plan-tag')) {
            el('dash-core-plan-tag').textContent = `PLAN 01 / ${(d.coreTrackCategory || 'CORE TRACK').toUpperCase()}`;
        }

        if (d.upNextLesson) {
            if (el('dash-upnext-title')) el('dash-upnext-title').textContent = d.upNextLesson.lessonTitle || 'Start your first lesson';
            if (el('dash-upnext-meta')) {
                el('dash-upnext-meta').textContent = 
                    `${d.upNextLesson.moduleTitle || 'Pipelines'} · ${d.upNextLesson.resourceType || 'Project'} · ${d.upNextLesson.durationMinutes || 90} min`;
            }
        }

        if (d.weeklyActivity) {
            this.renderWeeklyChart(d.weeklyActivity);
        }

        if (d.otherPlans) {
            this.renderOtherPlans(d.otherPlans);
        }

        if (d.recentBadges) {
            this.renderDashBadges(d.recentBadges);
        }

        if (d.spotlightDiscussion) {
            this.renderDashSpotlight(d.spotlightDiscussion);
        }

        if (d.miniLeaderboard) {
            this.renderMiniLeaderboard(d.miniLeaderboard);
        }
    },

    renderWeeklyChart(w) {
        const container = document.getElementById('weekly-bars-container');
        if (!container || !w.dailyLogs) return;

        container.innerHTML = '';
        const maxMinutes = 100;

        w.dailyLogs.forEach(log => {
            const col = document.createElement('div');
            col.className = 'chart-bar-col';

            const heightPct = Math.min(100, Math.max(4, Math.round((log.minutes / maxMinutes) * 100)));
            const isTargetDay = !!(w.targetMet && w.targetMetDayAbbr && log.day === w.targetMetDayAbbr);

            col.innerHTML = `
                <div class="chart-bar-fill ${isTargetDay ? 'target-day' : ''}" style="height: ${heightPct}px;" title="${log.minutes} mins"></div>
                <span class="chart-day-label">${log.day}</span>
            `;
            container.appendChild(col);
        });

        const captionEl = document.getElementById('weekly-chart-caption');
        if (captionEl) {
            if (w.targetMet && w.targetMetDay) {
                captionEl.textContent = `${w.totalMinutesLogged} of ${w.targetMinutes} minutes logged. Target met on ${w.targetMetDay}! 🎉`;
            } else {
                const remaining = Math.max(0, (w.targetMinutes || 240) - (w.totalMinutesLogged || 0));
                captionEl.textContent = `${w.totalMinutesLogged || 0} of ${w.targetMinutes || 240} minutes logged. ${remaining} minutes remaining.`;
            }
        }
    },

    renderOtherPlans(plans) {
        const list = document.getElementById('other-plans-list');
        if (!list) return;

        if (!plans || plans.length === 0) {
            list.innerHTML = `
                <div class="dash-empty-plans">
                    <p>No additional tracks enrolled yet.</p>
                    <button type="button" class="btn-text-link" style="margin-top: 0.5rem; display: inline-block;" onclick="App.navigate('catalog')">Browse Course Catalog →</button>
                </div>
            `;
            return;
        }

        list.innerHTML = '';
        plans.forEach(p => {
            const pct = p.progressPercentage !== undefined ? p.progressPercentage : 0;
            const item = document.createElement('div');
            item.className = 'dash-plan-card';
            const lessonsDone = p.completedLessons !== undefined ? p.completedLessons : Math.round((pct / 100) * 8);
            const totalLessons = p.totalLessons || 8;
            item.innerHTML = `
                <div class="dash-plan-card-header">
                    <span class="dash-plan-card-tag">${this.escapeHtml(p.category || 'Curriculum')}</span>
                    <span class="dash-plan-card-pct">${pct}%</span>
                </div>
                <h4 class="dash-plan-card-title">${this.escapeHtml(p.title || 'Learning Track')}</h4>
                <div class="dash-plan-card-bar">
                    <div class="dash-plan-card-fill" style="width: ${pct}%;"></div>
                </div>
                <div class="dash-plan-card-footer">
                    <span class="dash-plan-card-lessons">${lessonsDone} / ${totalLessons} lessons</span>
                    <button type="button" class="dash-plan-card-action" onclick="App.navigate('plan')">Continue →</button>
                </div>
            `;
            list.appendChild(item);
        });
    },

    getBadgeIconSvg(iconKey) {
        const key = (iconKey || '').toLowerCase().trim();
        const svgs = {
            'award': `<svg viewBox="0 0 24 24" width="22" height="22" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="8" r="6"/><polyline points="8.21 13.89 7 23 12 20 17 23 15.79 13.88"/></svg>`,
            'target': `<svg viewBox="0 0 24 24" width="22" height="22" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"/><circle cx="12" cy="12" r="6"/><circle cx="12" cy="12" r="2"/></svg>`,
            'flame': `<svg viewBox="0 0 24 24" width="22" height="22" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round"><path d="M8.5 14.5A2.5 2.5 0 0 0 11 12c0-1.38-.5-2-1-3-1.072-2.143-.224-4.054 2-6 .5 2.5 2 4.9 4 6.5 2 1.6 3 3.5 3 5.5a7 7 0 1 1-14 0c0-1.153.433-2.294 1-3a2.5 2.5 0 0 0 2.5 2.5z"/></svg>`,
            'zap': `<svg viewBox="0 0 24 24" width="22" height="22" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round"><polygon points="13 2 3 14 12 14 11 22 21 10 12 10 13 2"/></svg>`,
            'cloud': `<svg viewBox="0 0 24 24" width="22" height="22" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round"><path d="M17.5 19H9a7 7 0 1 1 6.71-9h1.79a4.5 4.5 0 1 1 0 9Z"/></svg>`,
            'star': `<svg viewBox="0 0 24 24" width="22" height="22" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round"><polygon points="12 2 15.09 8.26 22 9.27 17 14.14 18.18 21.02 12 17.77 5.82 21.02 7 14.14 2 9.27 8.91 8.26 12 2"/></svg>`,
            'trophy': `<svg viewBox="0 0 24 24" width="22" height="22" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round"><path d="M6 9H4.5a2.5 2.5 0 0 1 0-5H6"/><path d="M18 9h1.5a2.5 2.5 0 0 0 0-5H18"/><path d="M4 22h16"/><path d="M10 14.66V17c0 .55-.47.98-.97 1.21C7.85 18.75 7 20.24 7 22"/><path d="M14 14.66V17c0 .55.47.98.97 1.21C16.15 18.75 17 20.24 17 22"/><path d="M18 2H6v7a6 6 0 0 0 12 0V2Z"/></svg>`,
            'crown': `<svg viewBox="0 0 24 24" width="22" height="22" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round"><path d="m2 4 3 12h14l3-12-6 7-4-7-4 7-6-7zm3 16h14"/></svg>`,
            'shield': `<svg viewBox="0 0 24 24" width="22" height="22" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round"><path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"/></svg>`
        };
        return svgs[key] || svgs['award'];
    },

    getBadgeColorClass(iconKey) {
        const key = (iconKey || '').toLowerCase().trim();
        const colors = {
            'award': 'badge-theme-amber',
            'target': 'badge-theme-rose',
            'flame': 'badge-theme-orange',
            'zap': 'badge-theme-indigo',
            'cloud': 'badge-theme-sky',
            'star': 'badge-theme-yellow',
            'trophy': 'badge-theme-emerald',
            'crown': 'badge-theme-purple',
            'shield': 'badge-theme-blue'
        };
        return colors[key] || 'badge-theme-amber';
    },

    renderDashBadges(badges) {
        const grid = document.getElementById('dash-badges-grid');
        if (!grid) return;

        if (!badges || badges.length === 0) {
            grid.innerHTML = `
                <div class="dash-empty-plans" style="grid-column: 1 / -1;">
                    <p>Complete lessons and daily quizzes to unlock achievement badges.</p>
                </div>
            `;
            return;
        }

        grid.innerHTML = '';
        badges.slice(0, 3).forEach(b => {
            const chip = document.createElement('div');
            chip.className = 'dash-badge-chip';
            const iconSvg = this.getBadgeIconSvg(b.icon);
            const colorClass = this.getBadgeColorClass(b.icon);
            chip.innerHTML = `
                <div class="dash-badge-icon ${colorClass}">
                    ${iconSvg}
                </div>
                <div class="dash-badge-info">
                    <div class="dash-badge-name">${this.escapeHtml(b.title)}</div>
                    <div class="dash-badge-desc">${this.escapeHtml(b.description || 'Achievement milestone unlocked')}</div>
                </div>
            `;
            grid.appendChild(chip);
        });
    },

    renderDashSpotlight(sp) {
        if (!sp) return;
        const badgeEl = document.getElementById('dash-spotlight-badge');
        const titleEl = document.getElementById('dash-spotlight-title');
        const descEl = document.getElementById('dash-spotlight-desc');

        if (badgeEl) {
            const aiPill = sp.hasAiAnswer ? '<span class="badge-ai-pill">⚡ AI Answered</span> ' : '';
            badgeEl.innerHTML = `${aiPill}${this.escapeHtml(sp.category || 'Architecture')}`;
        }
        if (titleEl) {
            titleEl.textContent = sp.title || 'Course Discussions';
        }
        if (descEl) {
            descEl.textContent = sp.snippet || 'Join the discussion with peers and technical mentors.';
        }
    },

    renderMiniLeaderboard(mini) {
        const list = document.getElementById('mini-leaderboard-list');
        if (!list || !mini || !mini.topThree) return;

        list.innerHTML = '';

        const createRowHtml = (entry, isSelf) => {
            const isRank1 = entry.rank === 1;
            const isRank2 = entry.rank === 2;
            const isRank3 = entry.rank === 3;
            let badgeClass = 'rank-other';
            let badgeText = `#${entry.rank}`;
            if (isRank1) {
                badgeClass = 'rank-gold';
                badgeText = '🥇';
            } else if (isRank2) {
                badgeClass = 'rank-silver';
                badgeText = '🥈';
            } else if (isRank3) {
                badgeClass = 'rank-bronze';
                badgeText = '🥉';
            }

            const highlightClass = isSelf ? 'active-user-highlight' : '';
            const youPill = isSelf ? '<span class="you-badge">YOU</span>' : '';
            const displayName = entry.name || entry.userName || 'Learner';
            const avatar = entry.avatar || `https://api.dicebear.com/7.x/bottts/svg?seed=${encodeURIComponent(displayName)}`;

            return `
                <div class="mini-leaderboard-item ${highlightClass}" data-user-id="${entry.userId}">
                    <div class="mini-rank-badge ${badgeClass}">${badgeText}</div>
                    <img src="${avatar}" alt="${this.escapeHtml(displayName)}" class="mini-user-avatar">
                    <div class="mini-user-details">
                        <div class="mini-user-name">
                            <span class="user-name-text">${this.escapeHtml(displayName)}</span>
                            ${youPill}
                        </div>
                        <div class="mini-user-role">${this.escapeHtml(entry.currentRoleTitle || 'Engineer')}</div>
                    </div>
                    <div class="mini-xp-pill">
                        <span>${(entry.weeklyXp || 0).toLocaleString()}</span>
                        <span class="mini-xp-label">XP</span>
                    </div>
                </div>
            `;
        };

        // Render Top 3 entries
        mini.topThree.forEach(entry => {
            const isSelf = entry.isCurrentUser || entry.userId === this.currentUserId;
            list.insertAdjacentHTML('beforeend', createRowHtml(entry, isSelf));
        });

        // If active user is outside the Top 3, show separator and their highlighted row
        if (!mini.activeUserInTopThree && mini.activeUserEntry) {
            const divider = `
                <div class="mini-divider">
                    <span class="divider-dots">···</span>
                    <span>YOUR STANDING</span>
                    <span class="divider-dots">···</span>
                </div>
            `;
            list.insertAdjacentHTML('beforeend', divider);
            list.insertAdjacentHTML('beforeend', createRowHtml(mini.activeUserEntry, true));
        }
    },

    openUpNextLesson() {
        this.navigate('plan');
    },

    // =========================================================================
    // VIEW 2: COURSE CATALOG
    // =========================================================================
    async loadCatalog() {
        // Fast instant render from session cache
        if (!this.courses || this.courses.length === 0) {
            try {
                const cached = sessionStorage.getItem('cp_courses');
                if (cached) {
                    this.courses = JSON.parse(cached);
                    this.renderCatalogGrid(this.courses);
                }
            } catch (e) {}
        }

        try {
            const [courses, enrolledData] = await Promise.all([
                API.getCourses(),
                this.currentUserId ? API.getEnrolledCourses(this.currentUserId).catch(err => {
                    console.warn('Could not fetch enrolled courses for catalog:', err);
                    return [];
                }) : Promise.resolve([])
            ]);

            this.courses = courses || [];
            try { sessionStorage.setItem('cp_courses', JSON.stringify(this.courses)); } catch (e) {}

            const userEnrollmentMap = new Map();
            if (Array.isArray(enrolledData)) {
                enrolledData.forEach(item => {
                    const en = item.enrollment || item;
                    const cid = (item.course && item.course.id) || en.courseId || item.courseId;
                    if (cid) {
                        userEnrollmentMap.set(cid, en);
                    }
                });
            }
            this.catalogEnrollmentMap = userEnrollmentMap;

            // Re-render based on current active catalog filter
            const activeChip = document.querySelector('.catalog-filters .filter-chip.active');
            const activeFilter = activeChip && activeChip.id === 'catalog-chip-completed' ? 'COMPLETED' : 'ALL';
            if (activeFilter === 'COMPLETED') {
                this.filterCatalog('COMPLETED', activeChip);
            } else {
                this.renderCatalogGrid(this.courses);
            }
        } catch (err) {
            console.error('Failed to load catalog:', err);
        }
    },

    filterCatalog(category, btnElement) {
        document.querySelectorAll('.catalog-filters .filter-chip').forEach(c => c.classList.remove('active'));
        if (btnElement) btnElement.classList.add('active');

        if (category === 'ALL') {
            this.renderCatalogGrid(this.courses);
        } else if (category === 'COMPLETED') {
            const filtered = (this.courses || []).filter(c => {
                const en = this.catalogEnrollmentMap ? this.catalogEnrollmentMap.get(c.id) : null;
                return en && (en.status === 'COMPLETED' || (en.progressPercentage && en.progressPercentage >= 100));
            });
            this.renderCatalogGrid(filtered);
        } else {
            const filtered = (this.courses || []).filter(c => 
                (c.category && c.category.toLowerCase().includes(category.toLowerCase())) ||
                (c.track && c.track.toLowerCase().includes(category.toLowerCase()))
            );
            this.renderCatalogGrid(filtered);
        }
    },

    renderCatalogGrid(courseList) {
        const grid = document.getElementById('catalog-course-grid');
        if (!grid) return;

        grid.innerHTML = '';

        if (!courseList || courseList.length === 0) {
            grid.innerHTML = `
                <div class="empty-state-box" style="grid-column: 1 / -1; padding: 3rem 1.5rem; text-align: center; background: var(--bg-card); border-radius: var(--radius-lg); border: 1px dashed var(--border-color);">
                    <div style="font-size: 2.5rem; margin-bottom: 0.75rem;">🎓</div>
                    <h3 style="font-size: 1.15rem; font-weight: 700; color: var(--text-primary); margin-bottom: 0.5rem;">No Completed Courses Yet</h3>
                    <p style="color: var(--text-muted); font-size: 0.9rem; max-width: 440px; margin: 0 auto 1.25rem;">
                        You haven't completed any courses yet in this track. Start by exploring the catalog, complete lessons, and celebrate your milestones!
                    </p>
                    <button class="btn-primary" onclick="App.filterCatalog('ALL', document.querySelector('.catalog-filters .filter-chip'))">
                        Browse All Courses
                    </button>
                </div>
            `;
            return;
        }

        courseList.forEach(c => {
            const en = this.catalogEnrollmentMap ? this.catalogEnrollmentMap.get(c.id) : null;
            const isCompleted = en && (en.status === 'COMPLETED' || (en.progressPercentage && en.progressPercentage >= 100));
            const isInProgress = !isCompleted && en && (en.progressPercentage > 0 || (en.completedLessonIds && en.completedLessonIds.length > 0));
            const progressPct = en ? Math.round(en.progressPercentage || 0) : 0;

            let statusPillHtml = '';
            let btnHtml = '';
            let cardExtraClass = '';

            if (isCompleted) {
                cardExtraClass = ' is-completed';
                statusPillHtml = `<span class="badge-status-completed">✓ COMPLETED</span>`;
                btnHtml = `
                    <div style="display: flex; gap: 0.5rem;">
                        <button class="btn-primary btn-completed-plan" style="flex: 1;" onclick="App.openPlan('${c.id}')">
                            ✓ Completed · Review
                        </button>
                        <button class="btn-secondary" style="padding: 0.5rem 0.75rem; white-space: nowrap;" title="Send Certificate to your email" onclick="App.sendCourseCertificate('${c.id}')">
                            🎓 Certificate
                        </button>
                    </div>
                `;
            } else if (isInProgress) {
                statusPillHtml = `<span class="badge-status-inprogress">${progressPct}% IN PROGRESS</span>`;
                btnHtml = `
                    <button class="btn-primary btn-full-width" onclick="App.openPlan('${c.id}')">
                        Continue Course (${progressPct}%) →
                    </button>
                `;
            } else {
                statusPillHtml = `<span class="badge-status-not-enrolled">NOT ENROLLED</span>`;
                btnHtml = `
                    <div style="display: flex; gap: 0.5rem;">
                        <button class="btn-primary" style="flex: 1;" onclick="App.enrollCourseDirect('${c.id}')">
                            Enroll in Track 🚀
                        </button>
                        <button class="btn-secondary" style="flex: 1;" onclick="App.openPlan('${c.id}')">
                            Overview
                        </button>
                    </div>
                `;
            }

            const card = document.createElement('div');
            card.className = `card catalog-card${cardExtraClass}`;
            card.innerHTML = `
                <div>
                    <div class="card-header-row" style="display: flex; justify-content: space-between; align-items: center; gap: 0.5rem; margin-bottom: 0.4rem;">
                        <span class="tag-plan-category" style="white-space: nowrap; overflow: hidden; text-overflow: ellipsis; max-width: 130px;">${this.escapeHtml(c.category || 'Engineering')}</span>
                        <div class="card-header-badges" style="display: flex; gap: 0.4rem; align-items: center; flex-shrink: 0; white-space: nowrap;">
                            ${statusPillHtml}
                            <span class="badge-pct-pill">${this.escapeHtml(c.difficultyLevel || 'INTERMEDIATE')}</span>
                        </div>
                    </div>
                    <h3 class="core-course-title" style="font-size: 1.15rem; margin-top: 0.6rem;">${this.escapeHtml(c.title)}</h3>
                    <p class="core-course-sub" style="margin-bottom: 1rem;">${this.escapeHtml(c.description)}</p>
                </div>
                <div>
                    <div style="display: flex; gap: 1rem; font-size: 0.75rem; font-family: var(--font-mono); color: var(--text-muted); margin-bottom: 1rem;">
                        <span>DURATION: ${c.estimatedHours}h</span>
                        <span style="color: var(--brand-primary); font-weight: bold;">+${c.xpReward} XP</span>
                        <span>RATING: ${c.rating || 4.9}</span>
                    </div>
                    ${btnHtml}
                </div>
            `;
            grid.appendChild(card);
        });
    },

    openPlan(courseId) {
        this.currentPlanId = courseId;
        this.navigate('plan');
    },

    async sendCourseCertificate(courseId) {
        try {
            const user = this.currentUser;
            const targetUser = (user && user.email) ? user.email : 'user email';
            await API.post(`/courses/${courseId}/certificate/send?userId=${encodeURIComponent(this.currentUserId || 'user_1')}`);
            alert(`🎓 Course Completion Certificate sent to ${targetUser} via Resend!`);
            await this.refreshEmailAudits();
            this.openEmailModal();
        } catch (e) {
            console.error('Failed to send certificate:', e);
            alert(`Could not send certificate: ${e.message}`);
        }
    },

    // =========================================================================
    // VIEW 3: PLAN OVERVIEW
    // =========================================================================
    async loadPlanOverview(courseId) {
        try {
            const [course, enrollment] = await Promise.all([
                API.getCourse(courseId),
                API.getEnrollment(courseId, this.currentUserId)
            ]);

            this.activeCourse = course;
            this.activeEnrollment = enrollment;
            this.renderPlanOverview(course, enrollment);
        } catch (err) {
            console.error('Failed to load plan overview:', err);
        }
    },

    renderPlanOverview(course, enrollment) {
        if (!course) return;

        document.getElementById('plan-hero-title').textContent = course.title;
        document.getElementById('plan-hero-desc').textContent = course.description;

        const completedSet = new Set((enrollment && enrollment.completedLessonIds) ? enrollment.completedLessonIds : []);
        let courseLessonsCount = 0;
        let courseDurationMins = 0;
        if (course.modules) {
            course.modules.forEach(m => {
                if (m.lessons) {
                    courseLessonsCount += m.lessons.length;
                    m.lessons.forEach(l => {
                        courseDurationMins += parseInt(l.durationMinutes || 20);
                    });
                }
            });
        }
        const totalLessons = (enrollment && enrollment.totalLessons > 0) 
            ? enrollment.totalLessons 
            : (courseLessonsCount > 0 ? courseLessonsCount : 13);
        const completedCount = completedSet.size;
        const pct = totalLessons > 0 ? Math.round((completedCount / totalLessons) * 100) : 0;
        
        let remainingHours = 6.0;
        if (enrollment && enrollment.remainingHours !== undefined && enrollment.remainingHours > 0) {
            remainingHours = enrollment.remainingHours;
        } else if (courseDurationMins > 0) {
            const fractionRemaining = totalLessons > 0 ? (1.0 - (completedCount / totalLessons)) : 1.0;
            remainingHours = Math.max(0, Math.round(((courseDurationMins * fractionRemaining) / 60.0) * 10.0) / 10.0);
        }

        document.getElementById('plan-core-pct').textContent = `${pct}%`;
        document.getElementById('plan-summary-total').textContent = totalLessons;
        document.getElementById('plan-summary-done').textContent = completedCount;
        document.getElementById('plan-summary-time').textContent = `${remainingHours} h`;
        document.getElementById('plan-summary-date').textContent = enrollment ? (enrollment.targetDate || '12 Nov 2026') : '12 Nov 2026';

        const certBtn = document.getElementById('btn-plan-certificate');
        const contBtn = document.getElementById('btn-plan-continue');
        if (certBtn) {
            certBtn.style.display = (pct >= 100) ? 'block' : 'none';
        }
        if (contBtn) {
            contBtn.textContent = (pct >= 100) ? '✓ Completed · Review Curriculum' : 'Continue where I left off';
        }

        const container = document.getElementById('modules-tree-container');
        if (!container) return;

        container.innerHTML = '';
        if (course.modules) {
            course.modules.forEach((mod, modIdx) => {
                const modCode = `M0${modIdx + 1}`;
                const modLessons = mod.lessons || [];
                const modDoneCount = modLessons.filter(l => completedSet.has(l.id)).length;

                const modBlock = document.createElement('div');
                modBlock.className = 'module-block';

                let lessonsHtml = '';
                modLessons.forEach(les => {
                    const isDone = completedSet.has(les.id);
                    const typeClass = `type-${(les.resourceType || 'reading').toLowerCase()}`;

                    lessonsHtml += `
                        <div class="lesson-row" onclick="App.openLessonModal('${course.id}', '${les.id}')">
                            <div class="lesson-row-left">
                                <input type="checkbox" class="lesson-checkbox" 
                                    ${isDone ? 'checked' : ''} 
                                    onclick="event.stopPropagation()"
                                    onchange="App.toggleLessonCheck('${course.id}', '${les.id}')">
                                <span class="lesson-title-text ${isDone ? 'completed' : ''}">
                                    ${this.escapeHtml(les.title)}
                                </span>
                            </div>
                            <div class="lesson-row-right">
                                <span class="type-pill ${typeClass}">${les.resourceType || 'READING'}</span>
                                <span class="lesson-duration">${les.durationMinutes} min</span>
                            </div>
                        </div>
                    `;
                });

                modBlock.innerHTML = `
                    <div class="module-top-row">
                        <div class="module-title-area">
                            <span class="module-code">${modCode}</span>
                            <h3 class="module-heading">${this.escapeHtml(mod.title)}</h3>
                        </div>
                        <span class="module-progress-stat">${modDoneCount}/${modLessons.length} DONE</span>
                    </div>
                    <div class="lessons-list">
                        ${lessonsHtml}
                    </div>
                `;

                container.appendChild(modBlock);
            });
        }
    },

    async toggleLessonCheck(courseId, lessonId) {
        this._pendingToggles = this._pendingToggles || new Set();
        if (this._pendingToggles.has(lessonId)) return;
        this._pendingToggles.add(lessonId);

        // Optimistic UI response: instantly update checkbox row styling
        const checkbox = document.querySelector(`.lesson-checkbox[onchange*="${lessonId}"]`);
        if (checkbox) {
            const row = checkbox.closest('.lesson-row');
            const titleEl = row ? row.querySelector('.lesson-title-text') : null;
            if (titleEl) titleEl.classList.toggle('completed');
        }

        const prevPct = (this.activeEnrollment && this.activeEnrollment.courseId === courseId)
            ? (this.activeEnrollment.progressPercentage || 0) : 0;

        // Instant in-memory state update for 0ms visual responsiveness
        if (this.activeEnrollment && this.activeEnrollment.courseId === courseId) {
            let list = Array.isArray(this.activeEnrollment.completedLessonIds)
                ? [...this.activeEnrollment.completedLessonIds] : [];
            const idx = list.indexOf(lessonId);
            if (idx >= 0) {
                list.splice(idx, 1);
            } else {
                list.push(lessonId);
            }
            this.activeEnrollment.completedLessonIds = list;
            this.activeEnrollment.completedLessonsCount = list.length;
            const total = this.activeEnrollment.totalLessons || 2;
            this.activeEnrollment.progressPercentage = Math.round((list.length / total) * 100);
            if (this.activeCourse) {
                this.renderPlanOverview(this.activeCourse, this.activeEnrollment);
            }
        }

        try {
            const updatedEnrollment = await API.toggleLesson(courseId, lessonId, this.currentUserId);
            if (updatedEnrollment) {
                this.activeEnrollment = updatedEnrollment;
                if (this.activeCourse) {
                    this.renderPlanOverview(this.activeCourse, updatedEnrollment);
                }
                if (updatedEnrollment.progressPercentage >= 100 && prevPct < 100) {
                    this.showCompletionModal(this.activeCourse, updatedEnrollment);
                }
            }

            try {
                sessionStorage.removeItem(`cp_dash_${this.currentUserId}`);
                sessionStorage.removeItem(`cp_my_learning_${this.currentUserId}`);
            } catch (e) {}
            this.dashboardData = null;
            this.catalogEnrollmentMap = null;
        } catch (err) {
            console.warn('Lesson sync notice:', err.message);
        } finally {
            this._pendingToggles.delete(lessonId);
        }
    },

    // =========================================================================
    // INTERACTIVE LESSON PLAYER & ACTIVITY COMPLETION TRACKER
    // =========================================================================
    openLessonModal(courseId, lessonId) {
        if (!this.activeCourse || !this.activeCourse.modules) return;

        let foundLesson = null;
        let foundModule = null;

        for (const mod of this.activeCourse.modules) {
            if (mod.lessons) {
                const l = mod.lessons.find(item => item.id === lessonId);
                if (l) {
                    foundLesson = l;
                    foundModule = mod;
                    break;
                }
            }
        }

        if (!foundLesson) return;
        this.activeModalLesson = { courseId, lessonId, lesson: foundLesson };

        // Set Title & Meta
        document.getElementById('modal-lesson-type').textContent = foundLesson.resourceType || 'READING';
        document.getElementById('modal-lesson-title').textContent = foundLesson.title;
        document.getElementById('modal-lesson-sub').textContent = `${foundModule.title} · ${foundLesson.durationMinutes} min`;

        // Check if previously completed
        const isDone = this.activeEnrollment && this.activeEnrollment.completedLessonIds && this.activeEnrollment.completedLessonIds.includes(lessonId);
        this.activityRequirementMet = !!isDone;

        // Reset and clear any existing activity timers
        this.clearActivityTimers();

        // Handle Video vs Reading Component
        const videoContainer = document.getElementById('modal-video-container');
        const videoIframe = document.getElementById('modal-video-iframe');
        const videoMeta = document.getElementById('modal-video-meta');
        const readingContainer = document.getElementById('modal-reading-content');
        const isVideo = foundLesson.resourceType === 'VIDEO' && foundLesson.videoUrl;

        if (isVideo) {
            let embedUrl = foundLesson.videoUrl;
            let ytId = null;
            const ytMatch = embedUrl.match(/(?:embed\/|v=|vi\/|youtu\.be\/|\/v\/)([a-zA-Z0-9_-]{11})/);
            if (ytMatch) {
                ytId = ytMatch[1];
                embedUrl = `https://www.youtube-nocookie.com/embed/${ytId}?enablejsapi=1&rel=0`;
            } else if (!embedUrl.includes('enablejsapi=1')) {
                embedUrl += (embedUrl.includes('?') ? '&' : '?') + 'enablejsapi=1';
            }
            videoIframe.src = embedUrl;
            videoContainer.style.display = 'block';

            if (videoMeta) {
                videoMeta.style.display = 'block';
                const metaTitle = document.getElementById('video-meta-title');
                const metaDesc = document.getElementById('video-meta-desc');
                const directBtn = document.getElementById('btn-video-direct-watch');
                if (metaTitle) metaTitle.innerHTML = `<span>🎬</span> ${this.escapeHtml(foundLesson.title)} (${foundLesson.durationMinutes} min)`;
                if (metaDesc) metaDesc.textContent = foundLesson.summary || 'Watch at least 80% of this video lesson to fulfill the requirement and earn your XP reward.';
                if (directBtn) {
                    directBtn.href = ytId ? `https://www.youtube.com/watch?v=${ytId}` : foundLesson.videoUrl;
                    directBtn.style.display = 'inline-flex';
                }
            }

            // Hide reading container completely for video components
            if (readingContainer) {
                readingContainer.style.display = 'none';
                readingContainer.innerHTML = '';
            }

            this.setupVideoActivityTracking(courseId, lessonId, isDone);
        } else {
            videoIframe.src = '';
            videoContainer.style.display = 'none';
            if (videoMeta) videoMeta.style.display = 'none';

            // Show reading container for reading / exercise / project components
            if (readingContainer) {
                readingContainer.style.display = 'block';
                const contentText = foundLesson.content || foundLesson.summary || 'Detailed lesson instructions and material are available for this module.';
                readingContainer.innerHTML = this.renderMarkdown(contentText);
            }

            this.setupReadingActivityTracking(courseId, lessonId, isDone);
        }

        // Update Complete Button UI
        this.updateModalCompleteButton(isDone);

        document.getElementById('lesson-player-modal').style.display = 'flex';
    },

    clearActivityTimers() {
        if (this._videoPollInterval) {
            clearInterval(this._videoPollInterval);
            this._videoPollInterval = null;
        }
        if (this._readingTimerInterval) {
            clearInterval(this._readingTimerInterval);
            this._readingTimerInterval = null;
        }
        const readingContainer = document.getElementById('modal-reading-content');
        if (readingContainer) {
            readingContainer.onscroll = null;
        }
    },

    setupVideoActivityTracking(courseId, lessonId, isDone) {
        const fill = document.getElementById('activity-progress-fill');
        const label = document.getElementById('activity-status-label');
        const metric = document.getElementById('activity-metric-text');
        const badge = document.getElementById('activity-badge-status');
        const icon = document.getElementById('activity-icon');

        if (isDone) {
            if (icon) icon.textContent = '✓';
            if (label) label.textContent = 'Lesson completed (+XP awarded)';
            if (badge) {
                badge.textContent = '✓ Completed';
                badge.style.background = 'rgba(16, 185, 129, 0.15)';
                badge.style.color = '#34d399';
                badge.style.borderColor = 'rgba(16, 185, 129, 0.3)';
            }
            if (fill) fill.style.width = '100%';
            if (metric) metric.textContent = 'Requirement Met · 100%';
            return;
        }

        if (icon) icon.textContent = '▶️';
        if (label) label.textContent = 'Video Activity: Watch ≥ 80% to auto-complete';
        if (badge) {
            badge.textContent = '🔒 In Progress';
            badge.style.background = 'rgba(239, 68, 68, 0.15)';
            badge.style.color = '#f87171';
            badge.style.borderColor = 'rgba(239, 68, 68, 0.3)';
        }
        if (fill) fill.style.width = '0%';
        if (metric) metric.textContent = 'Watch Progress: 0% / 80% required';

        this._videoSessionSeconds = 0;
        this._autoCompleted = false;

        // Progressive tracking: calculates real elapsed time in session
        let simulatedSeconds = 0;
        this._videoPollInterval = setInterval(() => {
            if (this.activityRequirementMet && this._autoCompleted) return;

            this._videoSessionSeconds++;
            simulatedSeconds += 10;
            const watchPct = Math.min(100, Math.round((simulatedSeconds / 100) * 80));

            if (fill) fill.style.width = `${watchPct}%`;
            if (metric) metric.textContent = `Watch Progress: ${watchPct}% / 80% required (${this._videoSessionSeconds}s active session)`;

            if (watchPct >= 80 && !this._autoCompleted) {
                this.markActivityRequirementMet(courseId, lessonId, 'VIDEO', watchPct, this._videoSessionSeconds, true);
            }
        }, 1200);
    },

    setupReadingActivityTracking(courseId, lessonId, isDone) {
        const fill = document.getElementById('activity-progress-fill');
        const label = document.getElementById('activity-status-label');
        const metric = document.getElementById('activity-metric-text');
        const badge = document.getElementById('activity-badge-status');
        const icon = document.getElementById('activity-icon');

        if (isDone) {
            if (icon) icon.textContent = '✓';
            if (label) label.textContent = 'Lesson completed (+XP awarded)';
            if (badge) {
                badge.textContent = '✓ Completed';
                badge.style.background = 'rgba(16, 185, 129, 0.15)';
                badge.style.color = '#34d399';
                badge.style.borderColor = 'rgba(16, 185, 129, 0.3)';
            }
            if (fill) fill.style.width = '100%';
            if (metric) metric.textContent = 'Requirement Met · 100%';
            return;
        }

        if (icon) icon.textContent = '📖';
        if (label) label.textContent = 'Reading Activity: Scroll through material (≥85%)';
        if (badge) {
            badge.textContent = '🔒 Locked';
            badge.style.background = 'rgba(239, 68, 68, 0.15)';
            badge.style.color = '#f87171';
            badge.style.borderColor = 'rgba(239, 68, 68, 0.3)';
        }
        if (fill) fill.style.width = '0%';
        if (metric) metric.textContent = 'Scroll Progress: 0% / 85%';

        this._readingSessionSeconds = 0;
        this._autoCompleted = false;
        this._readingTimerInterval = setInterval(() => {
            this._readingSessionSeconds++;
        }, 1000);

        // Crucial fix: The scrollbar is on .lesson-modal-body, NOT #modal-reading-content!
        const modalBody = document.querySelector('.lesson-modal-body');
        const contentContainer = document.getElementById('modal-reading-content');

        const onScrollCheck = () => {
            if (this.activityRequirementMet) return;
            const target = modalBody || contentContainer;
            if (!target) return;
            const maxScroll = target.scrollHeight - target.clientHeight;
            const pct = maxScroll > 0 ? Math.min(100, Math.round((target.scrollTop / maxScroll) * 100)) : 100;

            if (fill) fill.style.width = `${pct}%`;
            if (metric) metric.textContent = `Scroll Progress: ${pct}% / 85% (${this._readingSessionSeconds}s reading)`;

            if (pct >= 85 || (pct >= 60 && this._readingSessionSeconds >= 8)) {
                this.markActivityRequirementMet(courseId, lessonId, 'READING', pct, this._readingSessionSeconds, true);
            }
        };

        if (modalBody) modalBody.onscroll = onScrollCheck;
        if (contentContainer) contentContainer.onscroll = onScrollCheck;
    },

    async markActivityRequirementMet(courseId, lessonId, activityType, pct = 100, seconds = 20, autoComplete = true) {
        if (this.activityRequirementMet && this._autoCompleted) return;
        this.activityRequirementMet = true;
        this.clearActivityTimers();

        const fill = document.getElementById('activity-progress-fill');
        const label = document.getElementById('activity-status-label');
        const metric = document.getElementById('activity-metric-text');
        const badge = document.getElementById('activity-badge-status');
        const icon = document.getElementById('activity-icon');

        if (fill) fill.style.width = '100%';
        if (icon) icon.textContent = '🎉';
        if (label) label.textContent = 'Activity Requirement Met! Auto-Completed';
        if (badge) {
            badge.textContent = '✓ Completed';
            badge.style.background = 'rgba(16, 185, 129, 0.15)';
            badge.style.color = '#34d399';
            badge.style.borderColor = 'rgba(16, 185, 129, 0.3)';
        }
        if (metric) metric.textContent = `Completed (${pct}%) · Lesson Auto-Completed (+XP awarded)`;

        this.updateModalCompleteButton(true);

        // 1. Sync with backend activity endpoint (records real study time if seconds >= 30)
        API.recordLessonActivity(courseId, lessonId, this.currentUserId, activityType, pct, seconds)
            .catch(err => console.warn('Activity record notice:', err.message));

        // 2. Automatically mark as complete in enrollment if not already done
        const isDone = this.activeEnrollment && this.activeEnrollment.completedLessonIds && this.activeEnrollment.completedLessonIds.includes(lessonId);
        if (autoComplete && !isDone && !this._autoCompleted) {
            this._autoCompleted = true;
            try {
                const res = await API.toggleLesson(courseId, lessonId, this.currentUserId);
                if (res && res.data) {
                    this.activeEnrollment = res.data;
                    this.updateEnrollmentProgress(res.data);
                }
                const chk = document.getElementById(`chk-les-${lessonId}`);
                if (chk) chk.checked = true;

                // Fire celebration confetti & toast
                this.triggerCelebrationConfetti();
                const lessonTitle = this.activeModalLesson ? this.activeModalLesson.lesson.title : 'Lesson';
                this.showToast(`🎉 80% Complete! "${lessonTitle}" automatically marked complete (+XP awarded)!`, 'success');
            } catch (err) {
                console.warn('Auto-completion notice:', err.message);
            }
        }
    },

    devFastForwardActivity() {
        if (!this.activeModalLesson) return;
        const { courseId, lessonId, lesson } = this.activeModalLesson;
        this.markActivityRequirementMet(courseId, lessonId, lesson.resourceType || 'VIDEO', 80, 45, true);
    },

    updateModalCompleteButton(isDone) {
        const completeBtn = document.getElementById('btn-modal-complete');
        if (!completeBtn) return;

        if (isDone) {
            completeBtn.textContent = '✓ Completed (Auto-marked)';
            completeBtn.disabled = false;
            completeBtn.style.opacity = '1';
            completeBtn.style.cursor = 'pointer';
        } else if (this.activityRequirementMet) {
            completeBtn.textContent = '✓ Mark Lesson Complete (+XP)';
            completeBtn.disabled = false;
            completeBtn.style.opacity = '1';
            completeBtn.style.cursor = 'pointer';
        } else {
            completeBtn.textContent = '🔒 Complete Activity First';
            completeBtn.disabled = true;
            completeBtn.style.opacity = '0.6';
            completeBtn.style.cursor = 'not-allowed';
        }
    },

    closeLessonModal() {
        // If the user spent active real time in the session, log the real minutes
        const activeSeconds = (this._videoSessionSeconds || 0) + (this._readingSessionSeconds || 0);
        if (activeSeconds >= 30) {
            const mins = Math.max(1, Math.round(activeSeconds / 60));
            API.logActiveStudyTime(this.currentUserId, mins).catch(() => {});
        }
        this._videoSessionSeconds = 0;
        this._readingSessionSeconds = 0;
        this.clearActivityTimers();
        const modal = document.getElementById('lesson-player-modal');
        if (modal) modal.style.display = 'none';
        const videoIframe = document.getElementById('modal-video-iframe');
        if (videoIframe) videoIframe.src = '';
        this.activeModalLesson = null;
    },

    async toggleActiveModalLesson() {
        if (!this.activeModalLesson) return;
        const { courseId, lessonId } = this.activeModalLesson;
        await this.toggleLessonCheck(courseId, lessonId);
        this.closeLessonModal();
        this.refreshEmailAudits();
    },

    resumeFirstIncompleteLesson() {
        if (!this.activeCourse || !this.activeCourse.modules) return;
        const completedSet = new Set((this.activeEnrollment && this.activeEnrollment.completedLessonIds) ? this.activeEnrollment.completedLessonIds : []);
        
        for (const mod of this.activeCourse.modules) {
            if (mod.lessons) {
                for (const l of mod.lessons) {
                    if (!completedSet.has(l.id)) {
                        this.openLessonModal(this.activeCourse.id, l.id);
                        return;
                    }
                }
            }
        }
        alert('All lessons completed in this plan!');
    },

    // =========================================================================
    // RESEND TRANSACTIONAL EMAIL NOTIFICATIONS CENTER
    // =========================================================================
    async openEmailModal() {
        const modal = document.getElementById('email-inbox-modal');
        if (modal) modal.style.display = 'flex';
        await this.refreshEmailAudits();
    },

    closeEmailModal() {
        const modal = document.getElementById('email-inbox-modal');
        if (modal) modal.style.display = 'none';
    },

    switchEmailTab(tab) {
        ['history', 'preview', 'test'].forEach(t => {
            const btn = document.getElementById(`tab-email-${t}`);
            const pane = document.getElementById(`email-content-${t}`);
            if (btn) {
                if (t === tab) btn.classList.add('active');
                else btn.classList.remove('active');
            }
            if (pane) {
                pane.style.display = (t === tab) ? 'block' : 'none';
            }
        });
        if (tab === 'history') {
            this.refreshEmailAudits();
        }
    },

    async refreshEmailAudits() {
        try {
            const [status, emails] = await Promise.all([
                API.getNotificationStatus().catch(() => ({ provider: 'Resend (https://resend.com)', liveMode: false })),
                API.getRecentEmails().catch(() => [])
            ]);

            const statusData = status && status.data ? status.data : status;
            const subEl = document.getElementById('email-modal-status-sub');
            if (subEl) {
                subEl.textContent = `Provider: ${statusData.provider || 'Resend'} · Status: ${statusData.liveMode ? '🟢 LIVE DELIVERING' : '🟡 SIMULATION & AUDIT MODE'}`;
            }

            const pill = document.getElementById('resend-live-status-pill');
            if (pill) {
                if (statusData.liveMode) {
                    pill.textContent = '🟢 Live Delivery Active';
                    pill.style.background = 'rgba(16, 185, 129, 0.15)';
                    pill.style.color = '#34d399';
                    pill.style.borderColor = 'rgba(16, 185, 129, 0.3)';
                } else {
                    pill.textContent = '🟡 Simulation & Audit Mode';
                    pill.style.background = 'rgba(245, 158, 11, 0.15)';
                    pill.style.color = '#fbbf24';
                    pill.style.borderColor = 'rgba(245, 158, 11, 0.3)';
                }
            }

            const keyInput = document.getElementById('resend-api-key-input');
            if (keyInput && statusData.liveMode && statusData.maskedApiKey && !keyInput.value) {
                keyInput.placeholder = statusData.maskedApiKey;
            }

            const countEl = document.getElementById('email-count-pill');
            if (countEl) countEl.textContent = (emails || []).length;

            const dotEl = document.getElementById('email-nav-dot');
            if (dotEl) dotEl.style.display = (emails && emails.length > 0) ? 'block' : 'none';

            this.cachedDispatchedEmails = emails || [];
            this.renderEmailAuditList(this.cachedDispatchedEmails);
        } catch (e) {
            console.warn('Could not refresh emails:', e);
        }
    },

    async configureResendKey() {
        const input = document.getElementById('resend-api-key-input');
        const feedback = document.getElementById('resend-key-status-msg');
        const btn = document.getElementById('btn-connect-resend-key');
        const key = input ? input.value.trim() : '';

        if (!key) {
            alert('Please enter your Resend API Key (starts with re_...).');
            return;
        }

        if (btn) btn.textContent = 'Connecting...';
        try {
            const res = await API.configureResendApiKey(key);
            if (feedback) {
                feedback.style.display = 'block';
                feedback.style.background = 'rgba(16, 185, 129, 0.15)';
                feedback.style.color = '#34d399';
                feedback.style.border = '1px solid rgba(16, 185, 129, 0.3)';
                feedback.textContent = (res && res.message) ? res.message : '✓ Resend API key connected successfully! Live delivery active.';
            }
            await this.refreshEmailAudits();
        } catch (e) {
            if (feedback) {
                feedback.style.display = 'block';
                feedback.style.background = 'rgba(239, 68, 68, 0.15)';
                feedback.style.color = '#f87171';
                feedback.style.border = '1px solid rgba(239, 68, 68, 0.3)';
                feedback.textContent = `Error connecting API key: ${e.message}`;
            }
        } finally {
            if (btn) btn.textContent = 'Connect API Key';
        }
    },

    renderEmailAuditList(emails) {
        const list = document.getElementById('email-audit-list');
        if (!list) return;

        if (!emails || emails.length === 0) {
            list.innerHTML = `
                <div style="text-align: center; padding: 2.5rem 1rem; color: var(--text-muted);">
                    <div style="font-size: 2.5rem; margin-bottom: 0.5rem;">✉️</div>
                    <h4 style="margin: 0 0 0.5rem 0; color: var(--text-primary);">No Emails Dispatched Yet</h4>
                    <p style="font-size: 0.85rem; max-width: 400px; margin: 0 auto 1rem;">
                        Enroll in a course, achieve 100% course completion, unlock a level/badge milestone, or use the "Send Test Email" tab to trigger Resend notifications!
                    </p>
                    <button class="btn-secondary" onclick="App.switchEmailTab('test')">Send Test Notification →</button>
                </div>
            `;
            return;
        }

        list.innerHTML = emails.map(email => {
            const isLive = email.status === 'DELIVERED_LIVE';
            const statusClass = isLive ? 'color: #34d399; background: rgba(16, 185, 129, 0.15);' : 'color: #fbbf24; background: rgba(245, 158, 11, 0.15);';
            const statusLabel = isLive ? '✓ LIVE DELIVERED' : '🟡 SIMULATED AUDIT';
            const dateStr = email.dispatchedAt ? new Date(email.dispatchedAt).toLocaleTimeString() : 'Just now';

            return `
                <div class="card" style="padding: 1rem; border: 1px solid var(--border-color); background: var(--bg-card); display: flex; justify-content: space-between; align-items: center; gap: 1rem;">
                    <div>
                        <div style="display: flex; gap: 0.5rem; align-items: center; margin-bottom: 0.3rem;">
                            <span style="font-size: 0.7rem; font-weight: 700; padding: 2px 6px; border-radius: 4px; ${statusClass}">${statusLabel}</span>
                            <span style="font-size: 0.75rem; color: var(--brand-primary); font-weight: 600;">${this.escapeHtml(email.type)}</span>
                            <span style="font-size: 0.75rem; color: var(--text-muted);">${dateStr}</span>
                        </div>
                        <h4 style="margin: 0 0 0.25rem 0; font-size: 0.95rem; color: var(--text-primary);">${this.escapeHtml(email.subject)}</h4>
                        <div style="font-size: 0.8rem; color: var(--text-secondary);">
                            To: <code style="color: var(--text-primary);">${this.escapeHtml(email.recipient)}</code> · ID: <span style="font-family: monospace; font-size: 0.75rem;">${email.resendMessageId || 'N/A'}</span>
                        </div>
                    </div>
                    <div>
                        <button class="btn-secondary" style="padding: 0.4rem 0.8rem; font-size: 0.8rem; white-space: nowrap;" onclick="App.previewEmailHtml('${email.id}')">
                            Preview HTML 👁️
                        </button>
                    </div>
                </div>
            `;
        }).join('');
    },

    previewEmailHtml(emailId) {
        const found = (this.cachedDispatchedEmails || []).find(e => e.id === emailId);
        if (!found) return;

        const frame = document.getElementById('email-preview-frame');
        if (frame) {
            frame.srcdoc = found.htmlContent;
        }
        this.switchEmailTab('preview');
    },

    async sendTestEmail() {
        const input = document.getElementById('test-email-recipient');
        const btn = document.getElementById('btn-send-test-email');
        const result = document.getElementById('test-email-result');
        const to = input ? input.value.trim() : 'delivered@resend.dev';

        if (!to) {
            alert('Please enter a recipient email.');
            return;
        }

        btn.disabled = true;
        btn.textContent = 'Dispatching...';
        result.style.display = 'none';

        try {
            const statusRes = await API.getNotificationStatus();
            const statusData = statusRes && statusRes.data ? statusRes.data : statusRes;
            const isLive = statusData && statusData.liveMode;

            const res = await API.sendTestEmail(to, 'Verification Test from CareerPulse & Resend');
            const audit = res && res.data ? res.data : res;
            result.style.display = 'block';

            if (!isLive) {
                result.style.background = 'rgba(245, 158, 11, 0.12)';
                result.style.color = '#fbbf24';
                result.style.border = '1px solid rgba(245, 158, 11, 0.3)';
                result.innerHTML = `
                    <div style="font-weight: 600; margin-bottom: 4px;">🟡 Processed in Simulation & Audit Mode</div>
                    <div>Email simulated and logged in the <strong>Dispatched</strong> tab (Simulated ID: <code>${audit.resendMessageId || 'sim'}</code>).</div>
                    <div style="margin-top: 6px; font-size: 0.8rem; color: #fde68a; line-height: 1.45;">
                        ⚠️ <strong>Why didn't an email arrive in ${this.escapeHtml(to)}?</strong><br>
                        Currently running without a Resend API Key. To deliver real emails directly to your Gmail inbox, paste your Resend API Key in the <strong>Resend API Key Connection</strong> card above and click <em>Connect API Key</em>!
                    </div>
                `;
            } else if (audit.status && audit.status.startsWith('FAILED')) {
                result.style.background = 'rgba(239, 68, 68, 0.15)';
                result.style.color = '#f87171';
                result.style.border = '1px solid rgba(239, 68, 68, 0.3)';
                result.innerHTML = `
                    <div style="font-weight: 600; margin-bottom: 4px;">❌ Resend API Notice: ${this.escapeHtml(audit.status)}</div>
                    <div style="font-size: 0.8rem; margin-top: 4px; color: #fca5a5; line-height: 1.4;">
                        Tip: On Resend's free tier with <code>onboarding@resend.dev</code>, Resend only permits sending to the email registered on your Resend account. To send to any other address, verify your domain at <a href="https://resend.com/domains" target="_blank" style="color: #fff; text-decoration: underline;">resend.com/domains ↗</a>.
                    </div>
                `;
            } else {
                result.style.background = 'rgba(16, 185, 129, 0.15)';
                result.style.color = '#34d399';
                result.style.border = '1px solid rgba(16, 185, 129, 0.3)';
                result.innerHTML = `
                    <div style="font-weight: 600; margin-bottom: 4px;">✓ Live Email Dispatched via Resend!</div>
                    <div>Message ID: <code>${audit.resendMessageId || 'OK'}</code> sent to <strong>${this.escapeHtml(to)}</strong>. Check your inbox or spam folder!</div>
                `;
            }
            await this.refreshEmailAudits();
        } catch (e) {
            result.style.display = 'block';
            result.style.background = 'rgba(239, 68, 68, 0.15)';
            result.style.color = '#f87171';
            result.style.border = '1px solid rgba(239, 68, 68, 0.3)';
            result.innerHTML = `Error dispatching test email: ${e.message}`;
        } finally {
            btn.disabled = false;
            btn.textContent = 'Send Test Notification';
        }
    },

    async enrollInActiveCourse() {
        if (!this.activeCourse) return;
        await this.enrollCourseDirect(this.activeCourse.id);
    },

    async enrollCourseDirect(courseId) {
        try {
            const res = await API.enrollCourse(courseId, this.currentUserId);
            this.activeEnrollment = res;
            alert(`✉️ Successfully enrolled in course! Welcome email dispatched via Resend.`);
            await this.loadCatalog();
            await this.openPlan(courseId);
            this.refreshEmailAudits();
        } catch (e) {
            console.error('Enrollment failed:', e);
            alert('Could not complete enrollment. Please try again.');
        }
    },

    // =========================================================================
    // VIEW 4: PLAN BUILDER (NO-CODE SCHOLAR STUDIO)
    // =========================================================================
    
    async generateAiPlan() {
        const inputEl = document.getElementById('ai-prompt-input');
        const btnText = document.getElementById('ai-btn-text');
        const btnSpinner = document.getElementById('ai-spinner');
        const btnSubmit = document.getElementById('btn-ai-generate');
        const resultsContainer = document.getElementById('ai-udemy-results');
        const resultsList = document.getElementById('ai-udemy-list');
        const youtubeContainer = document.getElementById('ai-youtube-results');
        const youtubeList = document.getElementById('ai-youtube-list');
        
        const prompt = inputEl.value.trim();
        if (!prompt) {
            alert('Please enter what you want to learn.');
            return;
        }

        // Loading state
        btnText.style.display = 'none';
        btnSpinner.style.display = 'block';
        btnSubmit.disabled = true;
        
        try {
            const res = await API.generateAiPlan(prompt);
            const plan = res; 

            if (!plan) {
                throw new Error('No plan data received from AI service.');
            }

            // Fill meta fields
            this.builderState.title = plan.title || (prompt.charAt(0).toUpperCase() + prompt.slice(1) + ' Mastery');
            this.builderState.track = plan.track || 'Engineering';
            
            // Generate modules
            this.builderState.modules = [];
            if (plan.modules && plan.modules.length > 0) {
                plan.modules.forEach((mod, idx) => {
                    this.builderState.modules.push({
                        id: 'mod_' + Date.now() + '_' + idx,
                        title: mod.title || `Module ${idx + 1}`,
                        lessons: (mod.lessons || []).map((les, lIdx) => ({
                            id: 'les_' + Date.now() + '_' + lIdx,
                            title: les.title || `Lesson ${lIdx + 1}`,
                            resourceType: les.resourceType || 'READING',
                            durationMinutes: les.durationMinutes || 20,
                            content: les.content || 'Detailed lesson overview.',
                            videoUrl: les.videoUrl || ''
                        }))
                    });
                });
            }
            this.renderBuilderView();
            
            // Show Udemy recommendations
            if (plan.udemyRecommendations && plan.udemyRecommendations.length > 0) {
                resultsList.innerHTML = plan.udemyRecommendations.map(rec => `
                    <div class="udemy-card">
                        <div class="udemy-info">
                            <a href="${rec.url || '#'}" target="_blank" rel="noopener noreferrer" class="udemy-course-title">${this.escapeHtml(rec.title || 'Featured Course')}</a>
                            <span class="udemy-instructor">By ${this.escapeHtml(rec.instructor || 'Online Instructor')}</span>
                        </div>
                        <div class="udemy-meta">
                            <span class="udemy-rating">⭐ ${rec.rating || 4.7}</span>
                            ${rec.hasCertificate !== false ? '<span class="udemy-cert">✓ Certificate</span>' : ''}
                        </div>
                    </div>
                `).join('');
                resultsContainer.style.display = 'block';
            } else {
                resultsContainer.style.display = 'none';
            }
            
            // Show YouTube recommendations
            if (plan.youtubeRecommendations && plan.youtubeRecommendations.length > 0) {
                youtubeList.innerHTML = plan.youtubeRecommendations.map(rec => `
                    <div class="udemy-card" style="border-left: 3px solid #ff0000;">
                        <div class="udemy-info">
                            <a href="${rec.url || '#'}" target="_blank" rel="noopener noreferrer" class="udemy-course-title">${this.escapeHtml(rec.title || 'Featured Video')}</a>
                            <span class="udemy-instructor">Channel: ${this.escapeHtml(rec.channelName || 'YouTube')}</span>
                        </div>
                        <div class="udemy-meta">
                            <span class="udemy-cert" style="color: #ff0000; background: rgba(255,0,0,0.1);">▶ YouTube</span>
                            <span class="udemy-rating" style="margin-left: 0.5rem;">⏱ ${this.escapeHtml(rec.duration || 'Full Course')}</span>
                        </div>
                    </div>
                `).join('');
                youtubeContainer.style.display = 'block';
            } else {
                youtubeContainer.style.display = 'none';
            }
            
        } catch (err) {
            let msg = err.message || 'Unknown error occurred.';
            msg = msg.replace(/(AI generation failed:\s*)+/gi, '').trim();
            alert('AI Generation: ' + (msg || 'Unable to generate course plan right now. Please try again.'));
        } finally {
            // Restore state
            btnText.style.display = 'block';
            btnSpinner.style.display = 'none';
            btnSubmit.disabled = false;
        }
    },
    createNewCoursePlan() {
        this.builderState = {
            id: null,
            title: 'New Custom Course Plan',
            track: 'Engineering',
            category: 'Engineering',
            difficultyLevel: 'INTERMEDIATE',
            modules: [
                {
                    title: 'Module 1: Foundations',
                    lessons: [
                        { title: 'Core Principles Primer', resourceType: 'VIDEO', durationMinutes: 20, videoUrl: 'https://www.youtube-nocookie.com/embed/1FUcniACzmc', content: '### Foundations\n\nIntroductory concepts.' }
                    ]
                }
            ]
        };
        this.navigate('builder');
    },

    renderBuilderView() {
        const container = document.getElementById('builder-modules-list');
        if (!container) return;

        document.getElementById('builder-plan-title').value = this.builderState.title || '';
        document.getElementById('builder-plan-track').value = this.builderState.track || 'Data';
        document.getElementById('builder-mode-tag').textContent = this.builderState.id ? 'EDITING' : 'DRAFT';
        document.getElementById('builder-heading-title').textContent = this.builderState.id ? 'Edit Plan' : 'Plan builder';

        container.innerHTML = '';
        let totalModules = this.builderState.modules.length;
        let totalLessons = 0;
        let totalMinutes = 0;

        this.builderState.modules.forEach((mod, mIdx) => {
            const modCard = document.createElement('div');
            modCard.className = 'builder-module-card';

            totalLessons += mod.lessons.length;
            mod.lessons.forEach(l => totalMinutes += parseInt(l.durationMinutes || 20));

            let lessonsRows = '';
            mod.lessons.forEach((l, lIdx) => {
                lessonsRows += `
                    <div class="builder-lesson-row">
                        <span class="drag-handle">::</span>
                        <input type="text" class="form-input" value="${this.escapeHtml(l.title)}" 
                            onchange="App.updateBuilderLessonTitle(${mIdx}, ${lIdx}, this.value)" placeholder="Lesson title">
                        
                        <select class="form-select" onchange="App.updateBuilderLessonType(${mIdx}, ${lIdx}, this.value)">
                            <option value="VIDEO" ${l.resourceType === 'VIDEO' ? 'selected' : ''}>VIDEO</option>
                            <option value="READING" ${l.resourceType === 'READING' ? 'selected' : ''}>READING</option>
                            <option value="EXERCISE" ${l.resourceType === 'EXERCISE' ? 'selected' : ''}>EXERCISE</option>
                            <option value="PROJECT" ${l.resourceType === 'PROJECT' ? 'selected' : ''}>PROJECT</option>
                            <option value="LINK" ${l.resourceType === 'LINK' ? 'selected' : ''}>LINK</option>
                        </select>

                        <input type="number" class="form-input" style="width: 65px; font-family: var(--font-mono);" value="${l.durationMinutes}" 
                            onchange="App.updateBuilderLessonDuration(${mIdx}, ${lIdx}, this.value)">
                        
                        <input type="text" class="form-input" style="font-size: 0.75rem;" value="${this.escapeHtml(l.videoUrl || '')}" 
                            onchange="App.updateBuilderLessonVideo(${mIdx}, ${lIdx}, this.value)" placeholder="Video URL / Embed">

                        <button class="btn-text-danger" onclick="App.removeBuilderLesson(${mIdx}, ${lIdx})">✕</button>
                    </div>
                `;
            });

            modCard.innerHTML = `
                <div class="builder-module-top">
                    <div style="display: flex; align-items: center; gap: 0.5rem; flex: 1;">
                        <span class="module-code">M0${mIdx + 1}</span>
                        <input type="text" class="form-input" style="font-weight: 600; width: 60%;" value="${this.escapeHtml(mod.title)}" 
                            onchange="App.updateBuilderModuleTitle(${mIdx}, this.value)" placeholder="Module title">
                    </div>
                    <div style="display: flex; align-items: center; gap: 1rem;">
                        <span class="module-progress-stat">${mod.lessons.length} LESSONS</span>
                        <button class="btn-text-danger" onclick="App.removeBuilderModule(${mIdx})">Remove</button>
                    </div>
                </div>
                <div class="builder-lessons-container">
                    ${lessonsRows}
                </div>
                <button class="btn-add-lesson" onclick="App.addBuilderLesson(${mIdx})">
                    + Add lesson
                </button>
            `;

            container.appendChild(modCard);
        });

        const hours = (totalMinutes / 60.0).toFixed(1);
        document.getElementById('builder-rollup-modules').textContent = totalModules;
        document.getElementById('builder-rollup-lessons').textContent = totalLessons;
        document.getElementById('builder-rollup-time').textContent = `${hours} h`;
        document.getElementById('builder-rollup-status').textContent = this.builderState.id ? 'Published' : 'Draft';
        document.getElementById('btn-publish-plan').textContent = this.builderState.id ? 'Save & Update Plan' : 'Publish plan';
    },

    addBuilderModule() {
        const nextIdx = this.builderState.modules.length + 1;
        this.builderState.modules.push({
            title: `Module ${nextIdx}`,
            lessons: [
                { title: 'New Unit Lesson', resourceType: 'READING', durationMinutes: 20, content: '### Lesson Overview\n\nContent details here.' }
            ]
        });
        this.renderBuilderView();
    },

    removeBuilderModule(index) {
        this.builderState.modules.splice(index, 1);
        this.renderBuilderView();
    },

    addBuilderLesson(moduleIndex) {
        this.builderState.modules[moduleIndex].lessons.push({
            title: 'New Unit Lesson',
            resourceType: 'VIDEO',
            durationMinutes: 20,
            videoUrl: 'https://www.youtube-nocookie.com/embed/1FUcniACzmc'
        });
        this.renderBuilderView();
    },

    removeBuilderLesson(moduleIndex, lessonIndex) {
        this.builderState.modules[moduleIndex].lessons.splice(lessonIndex, 1);
        this.renderBuilderView();
    },

    updateBuilderModuleTitle(mIdx, val) {
        this.builderState.modules[mIdx].title = val;
    },

    updateBuilderLessonTitle(mIdx, lIdx, val) {
        this.builderState.modules[mIdx].lessons[lIdx].title = val;
    },

    updateBuilderLessonType(mIdx, lIdx, val) {
        this.builderState.modules[mIdx].lessons[lIdx].resourceType = val;
    },

    updateBuilderLessonVideo(mIdx, lIdx, val) {
        this.builderState.modules[mIdx].lessons[lIdx].videoUrl = val;
    },

    updateBuilderLessonDuration(mIdx, lIdx, val) {
        this.builderState.modules[mIdx].lessons[lIdx].durationMinutes = parseInt(val) || 20;
        this.renderBuilderView();
    },

    async publishPlan() {
        const btn = document.getElementById('btn-publish-plan');
        if (btn) btn.textContent = 'Saving...';

        try {
            const payload = {
                id: this.builderState.id,
                title: this.builderState.title || 'Platform Engineering Fundamentals',
                track: this.builderState.track || 'Data',
                category: 'Engineering',
                difficultyLevel: 'INTERMEDIATE',
                description: 'Comprehensive modular curriculum created with Career Pulse Plan Builder.',
                authorName: (this.currentUser && this.currentUser.name) ? this.currentUser.name : 'Shivam Gupta',
                authorId: (this.currentUser && this.currentUser.id) ? this.currentUser.id : this.currentUserId,
                status: 'PUBLISHED',
                modules: this.builderState.modules.map((m, mIdx) => ({
                    id: m.id,
                    title: m.title,
                    orderIndex: mIdx + 1,
                    lessons: m.lessons.map((l, lIdx) => ({
                        id: l.id,
                        title: l.title,
                        resourceType: l.resourceType,
                        durationMinutes: l.durationMinutes,
                        videoUrl: l.videoUrl,
                        content: l.content || `### ${l.title}\n\nTechnical lesson instructions.`,
                        summary: `Lesson ${lIdx + 1} of ${m.title}`,
                        orderIndex: lIdx + 1
                    }))
                }))
            };

            let saved;
            if (this.builderState.id) {
                saved = await API.updateCourse(this.builderState.id, payload);
            } else {
                saved = await API.createCourse(payload);
            }

            sessionStorage.removeItem('cp_courses');
            sessionStorage.removeItem(`cp_dash_${this.currentUserId}`);
            alert(`Course Plan "${saved.title}" has been successfully saved to database & synchronized to disk.`);
            if (btn) btn.textContent = 'Publish plan';
            await this.loadCatalog();
            this.navigate('my-courses');
        } catch (err) {
            alert('Publish error: ' + err.message);
            if (btn) btn.textContent = 'Publish plan';
        }
    },

    // =========================================================================
    // VIEW 5: MY COURSES MANAGEMENT STUDIO
    // =========================================================================
    async loadMyCourses() {
        const filterAndRender = (courseList) => {
            const currentUserId = this.currentUserId;
            const currentUserName = (this.currentUser && this.currentUser.name) ? this.currentUser.name.trim().toLowerCase() : '';
            return (courseList || []).filter(c => {
                if (c.authorId && currentUserId && c.authorId === currentUserId) return true;
                if (c.authorName && currentUserName && c.authorName.trim().toLowerCase() === currentUserName) return true;
                return false;
            });
        };

        // Instant render if courses are already available in memory
        if (this.courses && this.courses.length > 0) {
            this.renderMyCoursesTable(filterAndRender(this.courses));
        } else {
            try {
                const cached = sessionStorage.getItem('cp_courses');
                if (cached) {
                    this.courses = JSON.parse(cached);
                    this.renderMyCoursesTable(filterAndRender(this.courses));
                }
            } catch (e) {}
        }

        try {
            const courses = await API.getCourses();
            this.courses = courses || [];
            try { sessionStorage.setItem('cp_courses', JSON.stringify(this.courses)); } catch (e) {}
            this.renderMyCoursesTable(filterAndRender(this.courses));
        } catch (err) {
            console.error('Failed to load my courses:', err);
        }
    },

    renderMyCoursesTable(courseList) {
        const tbody = document.getElementById('my-courses-table-body');
        if (!tbody) return;

        tbody.innerHTML = '';

        if (!courseList || courseList.length === 0) {
            const currentUserName = (this.currentUser && this.currentUser.name) ? this.currentUser.name : 'you';
            tbody.innerHTML = `
                <tr>
                    <td colspan="7" style="text-align: center; padding: 3.5rem 1.5rem;">
                        <div style="max-width: 440px; margin: 0 auto; display: flex; flex-direction: column; align-items: center; gap: 0.85rem;">
                            <div style="width: 56px; height: 56px; border-radius: 50%; background: var(--bg-hover, rgba(99, 102, 241, 0.1)); display: flex; align-items: center; justify-content: center; font-size: 1.6rem;">
                                ✍️
                            </div>
                            <h3 style="font-size: 1.15rem; font-weight: 700; color: var(--text-primary); margin: 0;">No Authored Courses Yet</h3>
                            <p style="font-size: 0.88rem; color: var(--text-muted); margin: 0; line-height: 1.5;">
                                No courses have been authored by <strong>${this.escapeHtml(currentUserName)}</strong> yet. 
                                Browse courses in the <strong>Catalog</strong> to enroll and learn, or design and publish your own curriculum in the Plan Builder!
                            </p>
                            <div style="display: flex; gap: 0.75rem; margin-top: 0.5rem;">
                                <button class="btn-primary" style="font-size: 0.85rem; padding: 0.5rem 1.1rem;" onclick="App.createNewCoursePlan()">
                                    + Create Course Plan
                                </button>
                                <button class="btn-secondary" style="font-size: 0.85rem; padding: 0.5rem 1.1rem;" onclick="App.navigate('catalog')">
                                    Browse Catalog
                                </button>
                            </div>
                        </div>
                    </td>
                </tr>
            `;
            return;
        }

        courseList.forEach(c => {
            const moduleCount = c.modules ? c.modules.length : 0;
            let lessonCount = 0;
            if (c.modules) {
                c.modules.forEach(m => {
                    if (m.lessons) lessonCount += m.lessons.length;
                });
            }

            const tr = document.createElement('tr');
            tr.innerHTML = `
                <td>
                    <div style="font-weight: 600;">${this.escapeHtml(c.title)}</div>
                    <div style="font-size: 0.72rem; font-family: var(--font-mono); color: var(--text-muted);">${c.id}</div>
                </td>
                <td><span class="type-pill">${this.escapeHtml(c.track || c.category || 'Data')}</span></td>
                <td style="font-family: var(--font-mono);">${moduleCount}</td>
                <td style="font-family: var(--font-mono);">${lessonCount}</td>
                <td style="font-family: var(--font-mono);">${c.estimatedHours}h</td>
                <td><span class="badge-status ${c.status || 'PUBLISHED'}">${c.status || 'PUBLISHED'}</span></td>
                <td>
                    <div class="table-actions-cell">
                        <button class="btn-table-action" onclick="App.openPlan('${c.id}')">Preview</button>
                        <button class="btn-table-action" onclick="App.editCourse('${c.id}')">Edit</button>
                        <button class="btn-table-action danger" onclick="App.deleteCourse('${c.id}')">Delete</button>
                    </div>
                </td>
            `;
            tbody.appendChild(tr);
        });
    },

    async editCourse(courseId) {
        try {
            const c = await API.getCourse(courseId);
            if (!c) return;

            // Authorization check: only author can edit
            const currentUserId = this.currentUserId;
            const currentUserName = (this.currentUser && this.currentUser.name) ? this.currentUser.name.trim().toLowerCase() : '';
            const isAuthor = (c.authorId && currentUserId && c.authorId === currentUserId) ||
                             (c.authorName && currentUserName && c.authorName.trim().toLowerCase() === currentUserName);
            if (!isAuthor) {
                alert(`Permission denied: Only the author (${c.authorName || 'Original Author'}) can edit this course.`);
                return;
            }

            this.builderState = {
                id: c.id,
                title: c.title,
                track: c.track || c.category || 'Data',
                category: c.category || 'Engineering',
                difficultyLevel: c.difficultyLevel || 'INTERMEDIATE',
                authorName: c.authorName,
                authorId: c.authorId,
                modules: c.modules ? c.modules.map(m => ({
                    id: m.id,
                    title: m.title,
                    lessons: m.lessons ? m.lessons.map(l => ({
                        id: l.id,
                        title: l.title,
                        resourceType: l.resourceType || 'READING',
                        durationMinutes: l.durationMinutes || 20,
                        videoUrl: l.videoUrl || '',
                        content: l.content || ''
                    })) : []
                })) : []
            };

            this.navigate('builder');
        } catch (err) {
            alert('Failed to load course for editing: ' + err.message);
        }
    },

    async deleteCourse(courseId) {
        let course = this.courses.find(item => item.id === courseId);
        if (!course) {
            try {
                course = await API.getCourse(courseId);
            } catch (e) {}
        }

        // Authorization check: only author can delete
        const currentUserId = this.currentUserId;
        const currentUserName = (this.currentUser && this.currentUser.name) ? this.currentUser.name.trim().toLowerCase() : '';
        if (course) {
            const isAuthor = (course.authorId && currentUserId && course.authorId === currentUserId) ||
                             (course.authorName && currentUserName && course.authorName.trim().toLowerCase() === currentUserName);
            if (!isAuthor) {
                alert(`Permission denied: Only the author (${course.authorName || 'Original Author'}) can delete this course.`);
                return;
            }
        }

        if (!confirm(`Are you sure you want to delete course plan "${courseId}"? This will remove it from the catalog and sync to disk.`)) {
            return;
        }

        try {
            await API.deleteCourse(courseId, this.currentUserId);
            sessionStorage.removeItem('cp_courses');
            sessionStorage.removeItem(`cp_dash_${this.currentUserId}`);
            alert('Course plan deleted successfully.');
            await this.loadMyCourses();
            await this.loadCatalog();
        } catch (err) {
            alert('Delete failed: ' + err.message);
        }
    },

    // =========================================================================
    // VIEW 6: TEAM & USER ANALYTICS
    // =========================================================================
    async loadTeamAnalytics() {
        try {
            const data = await API.getTeamAnalytics();
            this.teamData = data;
            this.renderTeamAnalytics(data);
        } catch (err) {
            console.error('Failed to load team analytics:', err);
        }
    },

    renderTeamAnalytics(d) {
        if (!d) return;

        document.getElementById('team-kpi-learners').textContent = d.totalLearners || 5;
        document.getElementById('team-kpi-avg-pct').textContent = `${d.averageCompletionRate || 60.6}%`;
        document.getElementById('team-kpi-hours').textContent = `${d.totalHoursLogged || 166} h`;
        document.getElementById('team-kpi-streaks').textContent = d.activeStreaksCount || 4;

        const tbody = document.getElementById('team-table-body');
        if (!tbody || !d.members) return;

        tbody.innerHTML = '';
        d.members.forEach(m => {
            const tr = document.createElement('tr');
            tr.innerHTML = `
                <td>
                    <div class="table-user-cell">
                        <img src="${m.avatar}" class="table-user-avatar" alt="${this.escapeHtml(m.name)}">
                        <div>
                            <div style="font-weight: 600;">${this.escapeHtml(m.name)}</div>
                            <div style="font-size: 0.72rem; color: var(--text-muted); font-family: var(--font-mono);">${this.escapeHtml(m.email)}</div>
                        </div>
                    </div>
                </td>
                <td>${this.escapeHtml(m.roleTitle)}</td>
                <td style="font-weight: 500;">${this.escapeHtml(m.coreTrackTitle)}</td>
                <td style="min-width: 130px;">
                    <div style="display: flex; align-items: center; gap: 0.65rem;">
                        <div class="plan-track-bar" style="flex: 1;">
                            <div class="plan-track-fill" style="width: ${m.progressPercentage}%;"></div>
                        </div>
                        <span style="font-family: var(--font-mono); font-size: 0.78rem;">${m.progressPercentage}%</span>
                    </div>
                </td>
                <td style="font-family: var(--font-mono);">${m.lessonsCompleted}/${m.totalLessons}</td>
                <td style="font-family: var(--font-mono);">${m.streakDays}d</td>
                <td style="font-family: var(--font-mono);">${m.hoursLogged} h</td>
                <td>
                    <span class="badge-status ${m.statusTag}">${m.statusTag.replace('_', ' ')}</span>
                </td>
            `;
            tbody.appendChild(tr);
        });
    },

    // Helpers
    renderMarkdown(text) {
        if (!text) return '';

        // 1. Extract and preserve code blocks
        const codeBlocks = [];
        let processed = text.replace(/```(?:([a-zA-Z0-9_-]+)\r?\n)?([\s\S]*?)```/g, (match, lang, code) => {
            const index = codeBlocks.length;
            codeBlocks.push({
                lang: (lang || 'code').toLowerCase(),
                code: this.escapeHtml(code.trimEnd())
            });
            return `@@@CODEBLOCK_${index}@@@`;
        });

        // 2. Escape HTML for the non-code text
        processed = this.escapeHtml(processed);

        // 3. Process GitHub-Flavored Markdown Tables
        processed = processed.replace(/(?:^|\n)(\|[^\n]+\|\r?\n\|[\s\-:|]+\|\r?\n(?:\|[^\n]+\|\r?\n?)+)/g, (match, tableText) => {
            const lines = tableText.trim().split(/\r?\n/).filter(l => l.trim().startsWith('|'));
            if (lines.length < 2) return match;

            const parseRow = (row) => {
                return row.split('|')
                    .slice(1, -1)
                    .map(cell => cell.trim());
            };

            const headerCells = parseRow(lines[0]);
            const bodyRows = lines.slice(2).map(parseRow);

            let tableHtml = '<div class="reading-table-wrapper"><table><thead><tr>';
            headerCells.forEach(cell => {
                let cellFormatted = cell.replace(/\*\*(.*?)\*\*/g, '<strong>$1</strong>');
                tableHtml += `<th>${cellFormatted}</th>`;
            });
            tableHtml += '</tr></thead><tbody>';

            bodyRows.forEach(row => {
                tableHtml += '<tr>';
                row.forEach(cell => {
                    let cellFormatted = cell
                        .replace(/\*\*(.*?)\*\*/g, '<strong>$1</strong>')
                        .replace(/\*([^*\n]+)\*/g, '<em>$1</em>')
                        .replace(/`([^`\n]+)`/g, '<code class="inline-code-pill">$1</code>');
                    tableHtml += `<td>${cellFormatted}</td>`;
                });
                tableHtml += '</tr>';
            });
            tableHtml += '</tbody></table></div>';
            return '\n\n' + tableHtml + '\n\n';
        });

        // 4. Blockquotes
        processed = processed.replace(/(?:^|\n)((?:&gt;[ ]?.*(?:\r?\n|$))+)/g, (match) => {
            const quoteLines = match.trim().split(/\r?\n/).map(l => l.replace(/^&gt;[ ]?/, ''));
            return `<blockquote>${quoteLines.join('<br>')}</blockquote>`;
        });

        // 5. Headings (ordered from longest #### to shortest # anchored at start of line)
        processed = processed.replace(/^####[ \t]+(.*?)$/gm, '<h4>$1</h4>');
        processed = processed.replace(/^###[ \t]+(.*?)$/gm, '<h3>$1</h3>');
        processed = processed.replace(/^##[ \t]+(.*?)$/gm, '<h2>$1</h2>');
        processed = processed.replace(/^#[ \t]+(.*?)$/gm, '<h2>$1</h2>');

        // 6. Horizontal rules
        processed = processed.replace(/^[ \t]*---[ \t]*$/gm, '<hr style="border: 0; border-top: 1px solid var(--border-color); margin: 1.25rem 0;">');

        // 7. Grouped Ordered Lists (e.g. 1. First Normal Form ...)
        processed = processed.replace(/(?:^|\n)((?:^\d+\.[ \t]+.*(?:\r?\n|$))+)/gm, (match) => {
            const items = match.trim().split(/\r?\n/).map(l => l.replace(/^\d+\.[ \t]+/, '').trim());
            return '<ol>' + items.map(item => `<li>${item}</li>`).join('') + '</ol>';
        });

        // 8. Grouped Unordered Lists (e.g. * Fact Tables ... or - Fact Tables ...)
        processed = processed.replace(/(?:^|\n)((?:^[-*][ \t]+.*(?:\r?\n|$))+)/gm, (match) => {
            const items = match.trim().split(/\r?\n/).map(l => l.replace(/^[-*][ \t]+/, '').trim());
            return '<ul>' + items.map(item => `<li>${item}</li>`).join('') + '</ul>';
        });

        // 9. Bold, Italics, Inline Code
        processed = processed.replace(/\*\*(.*?)\*\*/g, '<strong>$1</strong>');
        processed = processed.replace(/\*([^*\n]+)\*/g, '<em>$1</em>');
        processed = processed.replace(/`([^`\n]+)`/g, '<code class="inline-code-pill">$1</code>');

        // 10. Paragraphs & Linebreaks
        const paragraphs = processed.split(/\r?\n\s*\r?\n/);
        processed = paragraphs.map(p => {
            const trimmed = p.trim();
            if (!trimmed) return '';
            if (trimmed.startsWith('<h') || trimmed.startsWith('<table') || trimmed.startsWith('<div') ||
                trimmed.startsWith('<blockquote') || trimmed.startsWith('<ul') || trimmed.startsWith('<ol') ||
                trimmed.startsWith('<hr') || trimmed.startsWith('@@@CODEBLOCK_')) {
                return trimmed;
            }
            return `<p>${trimmed.replace(/\r?\n/g, '<br>')}</p>`;
        }).join('\n');

        // 11. Restore Code Blocks
        processed = processed.replace(/@@@CODEBLOCK_(\d+)@@@/g, (match, idx) => {
            const block = codeBlocks[parseInt(idx, 10)];
            if (!block) return '';
            return `
                <div class="code-snippet-box">
                    <div class="code-snippet-header">
                        <span>${block.lang}</span>
                    </div>
                    <pre><code>${block.code}</code></pre>
                </div>
            `;
        });

        return processed;
    },

    escapeHtml(str) {
        if (!str) return '';
        return String(str).replace(/[&<>'"]/g, tag => ({
            '&': '&amp;',
            '<': '&lt;',
            '>': '&gt;',
            "'": '&#39;',
            '"': '&quot;'
        }[tag] || tag));
    },

    // =========================================================================
    // VIEW 7: USER PROFILE
    // =========================================================================
    async loadProfile() {
        const user = this.currentUser;
        const d = this.dashboardData;

        if (user) {
            document.getElementById('profile-name').textContent = user.name || 'User';
            document.getElementById('profile-role-title').textContent = user.currentRoleTitle || 'Learner';
            document.getElementById('profile-email').textContent = user.email || '';
            document.getElementById('profile-avatar').src = user.avatar || `https://api.dicebear.com/7.x/bottts/svg?seed=${user.name}`;

            document.getElementById('profile-detail-name').textContent = user.name || 'User';
            document.getElementById('profile-detail-email').textContent = user.email || '';
            document.getElementById('profile-detail-role').textContent = user.currentRoleTitle || 'Learner';
            document.getElementById('profile-detail-target').textContent = user.targetRoleTitle || user.targetRoleId || 'Not Set';
            const profLevelEl = document.getElementById('profile-level');
            if (profLevelEl) profLevelEl.textContent = `LVL ${user.currentLevel || 1}`;
            const profLevelTitleEl = document.getElementById('profile-level-title');
            if (profLevelTitleEl) profLevelTitleEl.textContent = user.levelTitle || 'Novice Explorer';
        }

        const xpVal = (d && d.currentXp != null) ? d.currentXp : (user && user.currentXp != null ? user.currentXp : 0);
        const xpEl = document.getElementById('profile-xp');
        if (xpEl) xpEl.textContent = Number(xpVal).toLocaleString();

        if (d) {
            const profLevelEl = document.getElementById('profile-level');
            if (profLevelEl && d.currentLevel) profLevelEl.textContent = `LVL ${d.currentLevel}`;
            const profLevelTitleEl = document.getElementById('profile-level-title');
            if (profLevelTitleEl && d.levelTitle) profLevelTitleEl.textContent = d.levelTitle;

            const streakVal = (d.streakDays != null) ? d.streakDays : (user ? user.streakDays : 0);
            const streakEl = document.getElementById('profile-streak');
            if (streakEl) streakEl.textContent = `${streakVal}d`;

            const corePctEl = document.getElementById('profile-core-pct');
            if (corePctEl) corePctEl.textContent = `${d.coreTrackProgressPercentage != null ? d.coreTrackProgressPercentage : 0}%`;

            const coreTitleEl = document.getElementById('profile-core-title');
            if (coreTitleEl) coreTitleEl.textContent = d.coreTrackTitle || 'Core Track';

            const lessonsDoneEl = document.getElementById('profile-lessons-done');
            if (lessonsDoneEl) lessonsDoneEl.textContent = `${d.coreTrackLessonsDone != null ? d.coreTrackLessonsDone : 0} / ${d.coreTrackTotalLessons != null ? d.coreTrackTotalLessons : 0}`;

            const plansEnrolledEl = document.getElementById('profile-plans-enrolled');
            if (plansEnrolledEl) plansEnrolledEl.textContent = d.plansEnrolledCount != null ? d.plansEnrolledCount : '0';

            const hoursEl = document.getElementById('profile-hours');
            if (hoursEl) hoursEl.textContent = `${d.timeLoggedHours != null ? d.timeLoggedHours : 0} h`;

            const badgesCount = (user && user.unlockedBadgeIds) ? user.unlockedBadgeIds.length : (d && d.recentBadges ? d.recentBadges.length : 0);
            const badgesEl = document.getElementById('profile-badges');
            if (badgesEl) badgesEl.textContent = String(badgesCount);
        }

        // Count authored courses
        if (this.courses && user) {
            const currentUserId = user.id;
            const currentUserName = user.name ? user.name.trim().toLowerCase() : '';
            const authored = this.courses.filter(c => {
                if (c.authorId && currentUserId && c.authorId === currentUserId) return true;
                if (c.authorName && currentUserName && c.authorName.trim().toLowerCase() === currentUserName) return true;
                return false;
            }).length;
            document.getElementById('profile-courses-authored').textContent = authored;
        }

        // Load completed courses history in Profile
        await this.renderProfileCompletedCourses();
    },

    async renderProfileCompletedCourses() {
        const listEl = document.getElementById('profile-completed-courses-list');
        if (!listEl) return;

        try {
            const enrolled = await API.getEnrolledCourses(this.currentUserId);
            const completed = (enrolled || []).filter(i => i.enrollment && (i.enrollment.status === 'COMPLETED' || i.enrollment.progressPercentage >= 100));

            if (completed.length === 0) {
                listEl.innerHTML = `
                    <div style="padding: 1.5rem; text-align: center; color: var(--text-muted); font-size: 0.85rem;">
                        No courses completed yet. When you finish 100% of any course, it will automatically appear here with your verified completion timestamp!
                    </div>
                `;
                return;
            }

            listEl.innerHTML = completed.map(item => {
                const c = item.course;
                const e = item.enrollment;
                const completedDate = e.completedAt ? this.formatDateTime(e.completedAt) : 'Recently';
                const totalLessons = e.completedLessonsCount || e.totalLessons || (c.modules ? c.modules.reduce((a, m) => a + (m.lessons ? m.lessons.length : 0), 0) : 0);

                return `
                    <div class="profile-completed-course-row">
                        <div class="p-comp-info">
                            <div class="p-comp-badge">🏆</div>
                            <div>
                                <div class="p-comp-title">${this.escapeHtml(c.title)}</div>
                                <div class="p-comp-meta">
                                    <span>All ${totalLessons} lessons finished</span> · 
                                    <span>Track: ${this.escapeHtml(c.track || 'Engineering')}</span> · 
                                    <span class="p-comp-date">Completed on ${completedDate}</span>
                                </div>
                            </div>
                        </div>
                        <button class="btn-secondary btn-sm" onclick="App.navigateToPlan('${c.id}')">
                            View Plan
                        </button>
                    </div>
                `;
            }).join('');
        } catch (err) {
            console.error('Failed to render profile completed courses:', err);
        }
    },

    // =========================================================================
    // VIEW: MY LEARNING (ENROLLED COURSES)
    // =========================================================================
    async loadMyLearning(filter) {
        if (filter) {
            this.myLearningActiveFilter = filter;
        }

        // Fast instant render from session cache
        if (!this.myLearningCourses || this.myLearningUserId !== this.currentUserId) {
            try {
                const cached = sessionStorage.getItem(`cp_my_learning_${this.currentUserId}`);
                if (cached) {
                    this.myLearningCourses = JSON.parse(cached);
                    this.myLearningUserId = this.currentUserId;
                    this.renderMyLearning(this.myLearningActiveFilter);
                }
            } catch (e) {}
        }

        try {
            const enrolledData = await API.getEnrolledCourses(this.currentUserId);
            this.myLearningCourses = enrolledData || [];
            this.myLearningUserId = this.currentUserId;
            try {
                sessionStorage.setItem(`cp_my_learning_${this.currentUserId}`, JSON.stringify(this.myLearningCourses));
            } catch (e) {}
            this.renderMyLearning(this.myLearningActiveFilter);
        } catch (err) {
            console.error('Failed to load enrolled courses for My Learning:', err);
            const grid = document.getElementById('my-learning-grid');
            if (grid && (!this.myLearningCourses || this.myLearningCourses.length === 0)) {
                grid.innerHTML = `<div class="card" style="padding: 2rem; text-align: center; color: var(--text-secondary);">
                    Failed to load enrolled courses. Please try again.
                </div>`;
            }
        }
    },

    filterMyLearning(filter) {
        this.myLearningActiveFilter = filter;
        document.querySelectorAll('.my-learning-filters-bar .filter-chip').forEach(btn => {
            btn.classList.remove('active');
        });
        const activeChip = document.getElementById(`filter-chip-${filter}`);
        if (activeChip) activeChip.classList.add('active');
        this.renderMyLearning(filter);
    },

    renderMyLearning(filter = 'all') {
        const items = this.myLearningCourses || [];

        // Calculate KPIs
        const completedCourses = items.filter(item => item.enrollment && (item.enrollment.status === 'COMPLETED' || item.enrollment.progressPercentage >= 100));
        const inProgressCourses = items.filter(item => item.enrollment && item.enrollment.progressPercentage < 100);

        let totalCompletedLessons = 0;
        let totalInvestedHours = 0;

        items.forEach(item => {
            const en = item.enrollment;
            const cr = item.course;
            const completedCount = (en && en.completedLessonIds) ? en.completedLessonIds.length : (en ? en.completedLessonsCount || 0 : 0);
            totalCompletedLessons += completedCount;

            // Calculate hours invested
            if (cr && cr.modules && en && en.completedLessonIds) {
                const doneSet = new Set(en.completedLessonIds);
                cr.modules.forEach(m => {
                    if (m.lessons) {
                        m.lessons.forEach(l => {
                            if (doneSet.has(l.id)) {
                                totalInvestedHours += (l.durationMinutes || 20) / 60.0;
                            }
                        });
                    }
                });
            }
        });

        // Update KPI summary cards
        const kpiCompEl = document.getElementById('my-learning-kpi-completed');
        const kpiInprogEl = document.getElementById('my-learning-kpi-inprogress');
        const kpiLessonsEl = document.getElementById('my-learning-kpi-lessons');
        const kpiHoursEl = document.getElementById('my-learning-kpi-hours');

        if (kpiCompEl) kpiCompEl.textContent = completedCourses.length;
        if (kpiInprogEl) kpiInprogEl.textContent = inProgressCourses.length;
        if (kpiLessonsEl) kpiLessonsEl.textContent = totalCompletedLessons;
        if (kpiHoursEl) kpiHoursEl.textContent = `${Math.round(totalInvestedHours * 10) / 10} h`;

        // Update chip badges
        const chipAll = document.getElementById('count-chip-all');
        const chipComp = document.getElementById('count-chip-completed');
        const chipInprog = document.getElementById('count-chip-inprogress');
        if (chipAll) chipAll.textContent = items.length;
        if (chipComp) chipComp.textContent = completedCourses.length;
        if (chipInprog) chipInprog.textContent = inProgressCourses.length;

        // Apply filtering
        let displayList = items;
        if (filter === 'completed') {
            displayList = completedCourses;
        } else if (filter === 'inprogress') {
            displayList = inProgressCourses;
        }

        const grid = document.getElementById('my-learning-grid');
        if (!grid) return;

        if (displayList.length === 0) {
            grid.innerHTML = `
                <div class="card" style="padding: 3rem 2rem; text-align: center;">
                    <div style="font-size: 2.5rem; margin-bottom: 0.75rem;">📚</div>
                    <h3 style="font-size: 1.15rem; font-weight: 700; margin-bottom: 0.35rem;">No Courses in this Category</h3>
                    <p style="color: var(--text-secondary); font-size: 0.85rem; max-width: 420px; margin: 0 auto 1.25rem auto;">
                        ${filter === 'completed' 
                            ? 'You have not completed any courses yet. Finish 100% of the lessons in any enrolled course to celebrate your certification!' 
                            : 'Explore our catalog to enroll in exciting career plans.'}
                    </p>
                    <button class="btn-primary" onclick="App.navigate('catalog')">Browse Catalog →</button>
                </div>
            `;
            return;
        }

        grid.innerHTML = displayList.map(item => {
            const c = item.course;
            const e = item.enrollment || {};
            const isCompleted = e.status === 'COMPLETED' || e.progressPercentage >= 100;
            const pct = isCompleted ? 100 : (e.progressPercentage || 0);
            const completedLessonIds = new Set(e.completedLessonIds || []);

            // Collect all lessons for this course
            const allLessons = [];
            if (c.modules) {
                c.modules.forEach(m => {
                    if (m.lessons) {
                        m.lessons.forEach(l => {
                            allLessons.push({
                                ...l,
                                moduleTitle: m.title,
                                isCompleted: completedLessonIds.has(l.id)
                            });
                        });
                    }
                });
            }

            const completedLessonsList = allLessons.filter(l => l.isCompleted);
            const completionDateFormatted = isCompleted && e.completedAt 
                ? this.formatDateTime(e.completedAt) 
                : (isCompleted ? 'Recently' : '');

            let completionBannerHtml = '';
            if (isCompleted) {
                completionBannerHtml = `
                    <div class="mylearning-completion-banner">
                        <div class="mylearning-completion-banner-left">
                            <span class="mylearning-completion-icon">🏆</span>
                            <div>
                                <div class="mylearning-completion-title">Certified Completion (100%)</div>
                                <div class="mylearning-completion-date">Completed on: <strong>${completionDateFormatted}</strong></div>
                            </div>
                        </div>
                        <button class="btn-toggle-lessons" onclick="App.toggleCompletedLessonsDrawer('${c.id}')" id="btn-drawer-${c.id}">
                            <span>View All Completed Lessons (${completedLessonsList.length})</span>
                            <span id="chevron-${c.id}">▼</span>
                        </button>
                    </div>
                `;
            } else {
                completionBannerHtml = `
                    <div style="display: flex; justify-content: space-between; align-items: center; margin-top: 0.75rem;">
                        <span style="font-size: 0.78rem; color: var(--text-muted);">
                            ${completedLessonsList.length} of ${allLessons.length || e.totalLessons || 0} lessons finished
                        </span>
                        <button class="btn-toggle-lessons" onclick="App.toggleCompletedLessonsDrawer('${c.id}')" id="btn-drawer-${c.id}">
                            <span>View Lessons (${completedLessonsList.length}/${allLessons.length})</span>
                            <span id="chevron-${c.id}">▼</span>
                        </button>
                    </div>
                `;
            }

            // Lessons Drawer list items
            const lessonsDrawerHtml = allLessons.map(l => {
                const checkIcon = l.isCompleted ? '✓' : '○';
                const rowClass = l.isCompleted ? 'is-done' : '';
                return `
                    <div class="mylearning-lesson-item ${rowClass}">
                        <div class="mylearning-lesson-left">
                            <span class="lesson-check-glyph" style="${!l.isCompleted ? 'color: var(--text-muted);' : ''}">${checkIcon}</span>
                            <div>
                                <span class="mylearning-lesson-name">${this.escapeHtml(l.title)}</span>
                                <span class="mylearning-module-tag">${this.escapeHtml(l.moduleTitle)}</span>
                            </div>
                        </div>
                        <div class="mylearning-lesson-right">
                            <span class="type-pill type-${(l.resourceType || 'reading').toLowerCase()}">${l.resourceType || 'READING'}</span>
                            <span style="font-family: var(--font-mono); font-size: 0.75rem; color: var(--text-muted);">${l.durationMinutes || 20}m</span>
                            ${l.isCompleted ? '<span style="font-size: 0.72rem; color: var(--brand-success); font-weight: 700;">+25 XP</span>' : ''}
                        </div>
                    </div>
                `;
            }).join('');

            return `
                <div class="card my-learning-card ${isCompleted ? 'is-completed' : ''}" id="mylearning-card-${c.id}">
                    <div class="mylearning-card-header">
                        <div class="mylearning-card-main">
                            <div class="mylearning-top-meta">
                                <span class="plan-badge-tag">${this.escapeHtml(c.track || 'Engineering')}</span>
                                <span class="type-pill">${this.escapeHtml(c.category || 'Core')}</span>
                                ${isCompleted ? '<span class="type-pill" style="background: rgba(5,150,105,0.15); color: var(--brand-success); font-weight:700;">✓ 100% COMPLETED</span>' : ''}
                            </div>
                            <h2 class="mylearning-title">${this.escapeHtml(c.title)}</h2>
                            <p class="mylearning-desc">${this.escapeHtml(c.description || 'Curriculum plan.')}</p>
                        </div>
                        <div class="mylearning-card-action">
                            <button class="btn-primary btn-sm" onclick="App.navigateToPlan('${c.id}')">
                                ${isCompleted ? 'Review Plan' : 'Resume Plan →'}
                            </button>
                        </div>
                    </div>

                    <!-- Progress Bar Row -->
                    <div class="mylearning-progress-row">
                        <div class="mylearning-progress-bar-bg">
                            <div class="mylearning-progress-bar-fill ${isCompleted ? 'is-success' : ''}" style="width: ${pct}%"></div>
                        </div>
                        <span class="mylearning-pct-num ${isCompleted ? 'text-success' : ''}">${pct}%</span>
                    </div>

                    ${completionBannerHtml}

                    <!-- Collapsible Lessons Drawer -->
                    <div class="mylearning-lessons-drawer" id="drawer-lessons-${c.id}" style="display: none;">
                        ${lessonsDrawerHtml}
                    </div>
                </div>
            `;
        }).join('');
    },

    toggleCompletedLessonsDrawer(courseId) {
        const drawer = document.getElementById(`drawer-lessons-${courseId}`);
        const chevron = document.getElementById(`chevron-${courseId}`);
        if (!drawer) return;

        if (drawer.style.display === 'none' || drawer.style.display === '') {
            drawer.style.display = 'flex';
            if (chevron) chevron.textContent = '▲';
        } else {
            drawer.style.display = 'none';
            if (chevron) chevron.textContent = '▼';
        }
    },

    navigateToPlan(courseId) {
        this.currentPlanId = courseId;
        this.navigate('plan');
    },

    // =========================================================================
    // COURSE COMPLETION CELEBRATION MODAL & CONFETTI ENGINE
    // =========================================================================
    showCompletionModal(course, enrollment) {
        if (!course || !enrollment) return;

        const modal = document.getElementById('course-completion-modal');
        if (!modal) return;

        const titleEl = document.getElementById('completion-modal-course-title');
        const learnerEl = document.getElementById('completion-modal-learner-name');
        const timeEl = document.getElementById('completion-modal-time');
        const xpEl = document.getElementById('completion-modal-xp');
        const lessonsEl = document.getElementById('completion-modal-lessons');

        if (titleEl) titleEl.textContent = course.title || 'Course Completed';
        if (learnerEl) learnerEl.textContent = (this.currentUser && this.currentUser.name) ? this.currentUser.name : 'Learner';
        
        const completedDateStr = enrollment.completedAt || new Date().toISOString();
        if (timeEl) timeEl.textContent = this.formatDateTime(completedDateStr);

        const totalLes = enrollment.totalLessons || (course.modules ? course.modules.reduce((acc, m) => acc + (m.lessons ? m.lessons.length : 0), 0) : 10);
        if (lessonsEl) lessonsEl.textContent = `${totalLes} / ${totalLes}`;
        
        const earnedXp = (course.xpReward && course.xpReward > 0) ? course.xpReward : (totalLes * 50);
        if (xpEl) xpEl.textContent = `+${earnedXp} XP`;

        modal.style.display = 'flex';
        this.triggerConfetti();
        this.syncUserGamification(true);
    },

    closeCompletionModal() {
        const modal = document.getElementById('course-completion-modal');
        if (modal) modal.style.display = 'none';

        if (this.confettiAnimationId) {
            cancelAnimationFrame(this.confettiAnimationId);
            this.confettiAnimationId = null;
        }

        const canvas = document.getElementById('confetti-canvas');
        if (canvas) {
            const ctx = canvas.getContext('2d');
            if (ctx) ctx.clearRect(0, 0, canvas.width, canvas.height);
        }
    },

    goToMyLearningFromCompletion() {
        this.closeCompletionModal();
        this.navigate('my-learning');
    },

    startConfetti(targetCanvasId) {
        this.triggerConfetti(targetCanvasId);
    },

    stopConfetti() {
        if (this.confettiAnimationId) {
            cancelAnimationFrame(this.confettiAnimationId);
            this.confettiAnimationId = null;
        }
        const canvas = document.getElementById('quiz-confetti-canvas') || document.getElementById('confetti-canvas');
        if (canvas) {
            const ctx = canvas.getContext('2d');
            if (ctx) ctx.clearRect(0, 0, canvas.width, canvas.height);
            canvas.style.display = 'none';
        }
    },

    triggerConfetti(targetCanvasId) {
        const canvas = targetCanvasId ? document.getElementById(targetCanvasId) : (document.getElementById('quiz-confetti-canvas') || document.getElementById('confetti-canvas'));
        if (!canvas) return;
        canvas.style.display = 'block';
        const ctx = canvas.getContext('2d');
        if (!ctx) return;

        // Resize canvas to full window
        canvas.width = window.innerWidth;
        canvas.height = window.innerHeight;

        if (this.confettiAnimationId) {
            cancelAnimationFrame(this.confettiAnimationId);
            this.confettiAnimationId = null;
        }

        const colors = ['#2563EB', '#3B82F6', '#059669', '#10B981', '#F59E0B', '#EF4444', '#8B5CF6', '#EC4899'];
        const particles = [];
        const count = 120;

        for (let i = 0; i < count; i++) {
            particles.push({
                x: canvas.width / 2 + (Math.random() - 0.5) * 160,
                y: canvas.height * 0.45 + (Math.random() - 0.5) * 80,
                vx: (Math.random() - 0.5) * 18,
                vy: (Math.random() * -14) - 6,
                size: Math.random() * 8 + 5,
                color: colors[Math.floor(Math.random() * colors.length)],
                rotation: Math.random() * 360,
                rotSpeed: (Math.random() - 0.5) * 12,
                opacity: 1,
                decay: Math.random() * 0.005 + 0.003,
                gravity: 0.38,
                shape: Math.random() > 0.4 ? 'rect' : 'circle'
            });
        }

        const startTime = Date.now();
        const duration = 4500;

        const animate = () => {
            const elapsed = Date.now() - startTime;
            if (elapsed > duration) {
                ctx.clearRect(0, 0, canvas.width, canvas.height);
                this.confettiAnimationId = null;
                canvas.style.display = 'none';
                return;
            }

            ctx.clearRect(0, 0, canvas.width, canvas.height);

            for (let i = 0; i < particles.length; i++) {
                const p = particles[i];
                p.x += p.vx;
                p.y += p.vy;
                p.vy += p.gravity;
                p.vx *= 0.98;
                p.rotation += p.rotSpeed;
                p.opacity -= p.decay;

                if (p.opacity <= 0) continue;

                ctx.save();
                ctx.globalAlpha = Math.max(0, p.opacity);
                ctx.translate(p.x, p.y);
                ctx.rotate((p.rotation * Math.PI) / 180);
                ctx.fillStyle = p.color;

                if (p.shape === 'rect') {
                    ctx.fillRect(-p.size / 2, -p.size / 4, p.size, p.size * 0.5);
                } else {
                    ctx.beginPath();
                    ctx.arc(0, 0, p.size / 2, 0, Math.PI * 2);
                    ctx.fill();
                }

                ctx.restore();
            }

            this.confettiAnimationId = requestAnimationFrame(animate);
        };

        this.confettiAnimationId = requestAnimationFrame(animate);
    },

    formatDateTime(dateVal) {
        if (!dateVal) return 'Recently';
        try {
            const d = new Date(dateVal);
            if (isNaN(d.getTime())) return String(dateVal);
            return new Intl.DateTimeFormat('en-US', {
                month: 'short',
                day: 'numeric',
                year: 'numeric',
                hour: 'numeric',
                minute: '2-digit',
                hour12: true
            }).format(d);
        } catch (e) {
            return String(dateVal);
        }
    },

    // =========================================================================
    // USER GAMIFICATION, XP, STREAK & LEVEL MODAL CONTROLLER
    // =========================================================================
    badgesCatalog: null,

    async syncUserGamification(forceRefresh = false) {
        if (!this.currentUserId) return;
        try {
            const user = await API.getUser(this.currentUserId, forceRefresh);
            if (user) {
                this.currentUser = { ...this.currentUser, ...user };
                try {
                    localStorage.setItem('career_pulse_auth_user', JSON.stringify(this.currentUser));
                } catch (e) {}
                await this.renderGamificationHUD(user);
            }
        } catch (err) {
            console.warn('Failed to sync gamification data:', err);
            if (this.currentUser) {
                await this.renderGamificationHUD(this.currentUser);
            }
        }
    },

    async renderGamificationHUD(user) {
        if (!user) return;

        const level = user.currentLevel || 1;
        const levelTitle = user.levelTitle || 'Novice Explorer';
        const currentXp = user.currentXp || 0;
        const streakDays = user.streakDays || 0;

        // 1. Update Header Level Pill
        const hudLevelVal = document.getElementById('hud-level-val');
        if (hudLevelVal) {
            hudLevelVal.textContent = `LVL ${level} · ${levelTitle.toUpperCase()}`;
        }

        // 2. Section 1: User Profile & Identity (Top Section)
        const avatarImg = document.getElementById('passport-user-avatar');
        if (avatarImg) {
            const avatarSrc = user.avatar || user.avatarUrl || `https://api.dicebear.com/7.x/bottts/svg?seed=${encodeURIComponent(user.name || user.id || 'Learner')}`;
            avatarImg.src = avatarSrc;
        }

        const passportName = document.getElementById('passport-user-name');
        if (passportName) {
            passportName.textContent = user.name || 'Learner';
        }

        const passportId = document.getElementById('passport-user-id');
        if (passportId) {
            passportId.textContent = (user.id || 'USER_1').toUpperCase();
        }

        const passportRole = document.getElementById('passport-user-role');
        if (passportRole) {
            passportRole.textContent = user.currentRoleTitle || user.role || 'Junior Data Engineer';
        }

        const passportEmail = document.getElementById('passport-user-email');
        if (passportEmail) {
            passportEmail.textContent = user.email || `${(user.id || 'user').toLowerCase()}@enterprise.io`;
        }

        // Skills Cloud & Count
        const skillsList = user.skills || [];
        const skillsCount = document.getElementById('passport-skills-count');
        if (skillsCount) {
            skillsCount.textContent = `${skillsList.length} ${skillsList.length === 1 ? 'Skill' : 'Skills'}`;
        }

        const skillsCloud = document.getElementById('passport-skills-cloud');
        if (skillsCloud) {
            if (skillsList.length === 0) {
                skillsCloud.innerHTML = '<span style="color: rgba(255,255,255,0.45); font-size: 0.8rem;">No custom competencies added yet. Click <strong>+ Add Skill</strong> above to track your skills!</span>';
            } else {
                skillsCloud.innerHTML = skillsList.map(s => {
                    const lvl = (s.proficiencyLevel || 'INTERMEDIATE').toLowerCase();
                    const name = s.skillName || s.name || s.skillId || 'Skill';
                    const sId = s.id ? s.id : (s.skillId || name);
                    return `<div class="passport-skill-pill">
                        <span class="skill-pill-name">${this.escapeHtml(name)}</span>
                        <span class="skill-level-tag ${lvl}">${lvl}</span>
                        <button class="btn-remove-skill-chip" onclick="App.deleteUserSkill(${typeof sId === 'number' ? sId : `'${sId}'`})" title="Remove skill">&times;</button>
                    </div>`;
                }).join('');
            }
        }

        // 3. Section 2: Milestone Badges Grid (Middle Section)
        await this.renderPassportBadges(user);

        // 4. Section 3: XP Hero & Full 1 to 5 Roadmap Track (Lower Section)
        const modalTotalXp = document.getElementById('modal-total-xp');
        if (modalTotalXp) {
            // Display formatted with comma: e.g. 1,780 (US locale replaces . with ,)
            modalTotalXp.textContent = currentXp.toLocaleString('en-US');
        }

        const modalLevelTitleBadge = document.getElementById('modal-level-title-badge');
        if (modalLevelTitleBadge) {
            modalLevelTitleBadge.textContent = levelTitle;
        }

        const modalStreakDays = document.getElementById('modal-streak-days');
        if (modalStreakDays) {
            modalStreakDays.textContent = streakDays;
        }

        // Milestone thresholds & titles
        // Level 1: 0 XP (Novice Explorer)
        // Level 2: 500 XP (Code Apprentice)
        // Level 3: 1,500 XP (Skill Specialist)
        // Level 4: 3,000 XP (Lead Architect)
        // Level 5: 5,000 XP (Grandmaster)
        const levelTitles = {
            1: 'Novice Explorer',
            2: 'Code Apprentice',
            3: 'Skill Specialist',
            4: 'Lead Architect',
            5: 'Grandmaster'
        };

        let nextThreshold = 500;
        let nextLevelNum = 2;

        if (level === 1) {
            nextThreshold = 500;
            nextLevelNum = 2;
        } else if (level === 2) {
            nextThreshold = 1500;
            nextLevelNum = 3;
        } else if (level === 3) {
            nextThreshold = 3000;
            nextLevelNum = 4;
        } else if (level === 4) {
            nextThreshold = 5000;
            nextLevelNum = 5;
        } else {
            nextThreshold = 5000;
            nextLevelNum = 5;
        }

        // Remaining XP Subtitle
        const modalRemainingXpText = document.getElementById('modal-remaining-xp-text');
        if (modalRemainingXpText) {
            if (level >= 5 || currentXp >= 5000) {
                modalRemainingXpText.textContent = 'Maximum Level Reached (Grandmaster)';
            } else {
                const remaining = Math.max(0, nextThreshold - currentXp);
                modalRemainingXpText.textContent = `${remaining.toLocaleString('en-US')} XP to Level ${nextLevelNum} (${levelTitles[nextLevelNum]})`;
            }
        }

        // Continuous Roadmap Progress Fill (0% to 100% across 5 nodes)
        // Node 1: 0%, Node 2: 25%, Node 3: 50%, Node 4: 75%, Node 5: 100%
        let roadmapPct = 0;
        if (currentXp <= 0) {
            roadmapPct = 3;
        } else if (currentXp < 500) {
            roadmapPct = 0 + (currentXp / 500) * 25;
        } else if (currentXp < 1500) {
            roadmapPct = 25 + ((currentXp - 500) / 1000) * 25;
        } else if (currentXp < 3000) {
            roadmapPct = 50 + ((currentXp - 1500) / 1500) * 25;
        } else if (currentXp < 5000) {
            roadmapPct = 75 + ((currentXp - 3000) / 2000) * 25;
        } else {
            roadmapPct = 100;
        }
        roadmapPct = Math.min(100, Math.max(3, roadmapPct));

        const roadmapFill = document.getElementById('modal-roadmap-fill');
        if (roadmapFill) {
            roadmapFill.style.width = `${roadmapPct.toFixed(1)}%`;
        }

        // Update 5 Milestone Nodes
        for (let i = 1; i <= 5; i++) {
            const nodeEl = document.getElementById(`roadmap-node-${i}`);
            if (nodeEl) {
                nodeEl.classList.remove('completed', 'active', 'locked');
                const iconSpan = nodeEl.querySelector('.node-status-icon');
                if (i < level) {
                    nodeEl.classList.add('completed');
                    if (iconSpan) iconSpan.textContent = '✓';
                } else if (i === level) {
                    nodeEl.classList.add('active');
                    if (iconSpan) iconSpan.textContent = '★';
                } else {
                    nodeEl.classList.add('locked');
                    if (iconSpan) iconSpan.textContent = '🔒';
                }
            }
        }
    },

    async renderPassportBadges(user) {
        const grid = document.getElementById('passport-badges-grid');
        const ratioBadge = document.getElementById('passport-badges-ratio');
        if (!grid) return;

        if (!this.badgesCatalog || this.badgesCatalog.length === 0) {
            try {
                const res = await API.getBadges();
                this.badgesCatalog = (res && res.data) ? res.data : (Array.isArray(res) ? res : []);
            } catch (e) {
                console.warn('Could not fetch badges catalog:', e);
            }
        }

        // Fallback default badges if catalog empty
        const catalog = (this.badgesCatalog && this.badgesCatalog.length > 0) ? this.badgesCatalog : [
            { id: 'BADGE_FIRST_STEP', title: 'First Step Forward', description: 'Completed your first learning module on the platform.', icon: 'award' },
            { id: 'BADGE_COURSES_3', title: 'Triple Threat', description: 'Completed 3 full courses on the platform.', icon: 'trophy' },
            { id: 'BADGE_COURSES_5', title: 'Master Scholar', description: 'Completed 5 full courses on the platform.', icon: 'crown' },
            { id: 'BADGE_XP_1000', title: 'XP Millennial', description: 'Crossed 1,000 total lifetime experience points.', icon: 'star' },
            { id: 'BADGE_STREAK_3', title: 'Consistent Learner', description: 'Maintained a 3-day continuous learning streak.', icon: 'flame' },
            { id: 'BADGE_STREAK_7', title: 'Streak Titan', description: 'Maintained a 7-day continuous learning streak.', icon: 'zap' }
        ];

        const unlockedIds = new Set(user.unlockedBadgeIds || []);
        let unlockedCount = 0;

        const iconEmojiMap = {
            'award': '🎖️',
            'trophy': '🏆',
            'crown': '👑',
            'star': '⭐',
            'flame': '🔥',
            'zap': '⚡',
            'target': '🎯',
            'cloud': '☁️'
        };

        grid.innerHTML = catalog.map(b => {
            const isUnlocked = unlockedIds.has(b.id);
            if (isUnlocked) unlockedCount++;
            const emoji = iconEmojiMap[b.icon] || '🏅';
            return `<div class="passport-badge-card ${isUnlocked ? 'unlocked' : 'locked'}">
                <div class="badge-card-icon">${emoji}</div>
                <div class="badge-card-title">${this.escapeHtml(b.title)}</div>
                <div class="badge-card-desc">${this.escapeHtml(b.description)}</div>
                <span class="badge-card-status-pill ${isUnlocked ? 'unlocked-pill' : 'locked-pill'}">
                    ${isUnlocked ? '✓ UNLOCKED' : '🔒 LOCKED'}
                </span>
            </div>`;
        }).join('');

        if (ratioBadge) {
            ratioBadge.textContent = `${unlockedCount} / ${catalog.length} Unlocked`;
        }
    },

    toggleAddSkillForm(show) {
        const form = document.getElementById('passport-add-skill-form');
        if (!form) return;
        const shouldShow = (show !== undefined) ? show : form.style.display === 'none';
        form.style.display = shouldShow ? 'block' : 'none';
        if (shouldShow) {
            const input = document.getElementById('input-new-skill-name');
            if (input) input.focus();
        }
    },

    async saveNewSkill() {
        const input = document.getElementById('input-new-skill-name');
        const select = document.getElementById('select-new-skill-level');
        if (!input || !input.value.trim()) {
            alert('Please enter a skill name (e.g. Python, Docker, React, AWS)');
            return;
        }
        const skillName = input.value.trim();
        const proficiency = select ? select.value : 'INTERMEDIATE';

        try {
            const res = await API.addUserSkill(this.currentUserId, {
                skillName: skillName,
                proficiencyLevel: proficiency,
                score: proficiency === 'EXPERT' ? 95 : proficiency === 'ADVANCED' ? 85 : proficiency === 'INTERMEDIATE' ? 70 : 50
            });
            const updatedUser = (res && res.data) ? res.data : (res && res.user ? res.user : null);
            if (updatedUser) {
                this.currentUser = { ...this.currentUser, ...updatedUser };
                await this.renderGamificationHUD(this.currentUser);
            } else {
                await this.syncUserGamification(true);
            }
            input.value = '';
            this.toggleAddSkillForm(false);
        } catch (err) {
            console.error('Failed to add skill:', err);
            alert('Failed to add skill: ' + (err.message || err));
        }
    },

    async deleteUserSkill(skillId) {
        if (!confirm('Remove this skill from your profile competencies?')) return;
        try {
            const res = await API.removeUserSkill(this.currentUserId, skillId);
            const updatedUser = (res && res.data) ? res.data : (res && res.user ? res.user : null);
            if (updatedUser) {
                this.currentUser = { ...this.currentUser, ...updatedUser };
                await this.renderGamificationHUD(this.currentUser);
            } else {
                await this.syncUserGamification(true);
            }
        } catch (err) {
            console.error('Failed to delete skill:', err);
            alert('Failed to delete skill: ' + (err.message || err));
        }
    },

    async promptChangeAvatar() {
        const defaultSeeds = ['TechLead', 'CodeNinja', 'DataWizard', 'CyberSamurai', 'CloudPioneer', 'Shivam', 'QuantumDev'];
        const randomSeed = defaultSeeds[Math.floor(Math.random() * defaultSeeds.length)];
        const current = (this.currentUser && (this.currentUser.avatar || this.currentUser.avatarUrl)) || `https://api.dicebear.com/7.x/bottts/svg?seed=${randomSeed}`;
        const newUrl = prompt('Enter image URL for your profile picture (or leave blank for a new random Bottts avatar):', current);
        if (newUrl === null) return; // User cancelled
        
        const finalUrl = newUrl.trim() || `https://api.dicebear.com/7.x/bottts/svg?seed=${Math.random().toString(36).substring(7)}`;
        try {
            const res = await API.updateUserAvatar(this.currentUserId, finalUrl);
            const updatedUser = (res && res.data) ? res.data : (res && res.user ? res.user : null);
            if (updatedUser) {
                this.currentUser = { ...this.currentUser, ...updatedUser };
                const sidebarAvatar = document.getElementById('sidebar-avatar');
                if (sidebarAvatar) sidebarAvatar.src = finalUrl;
                await this.renderGamificationHUD(this.currentUser);
            } else {
                await this.syncUserGamification(true);
            }
        } catch (err) {
            console.error('Failed to update avatar:', err);
            alert('Failed to update avatar: ' + (err.message || err));
        }
    },

    escapeHtml(text) {
        if (!text) return '';
        return String(text)
            .replace(/&/g, '&amp;')
            .replace(/</g, '&lt;')
            .replace(/>/g, '&gt;')
            .replace(/"/g, '&quot;')
            .replace(/'/g, '&#039;');
    },

    toggleLevelModal() {
        const modal = document.getElementById('level-xp-modal');
        if (modal && modal.style.display !== 'none') {
            this.closeLevelModal();
        } else {
            this.openLevelModal();
        }
    },

    openLevelModal() {
        const modal = document.getElementById('level-xp-modal');
        if (modal) {
            modal.style.display = 'flex';
            this.syncUserGamification(true);
        }
    },

    closeLevelModal() {
        const modal = document.getElementById('level-xp-modal');
        if (modal) {
            modal.style.display = 'none';
        }
    },

    handleLevelModalOverlayClick(event) {
        if (event.target && event.target.id === 'level-xp-modal') {
            this.closeLevelModal();
        }
    },

    // =========================================================================
    // VIEW: COMPANY LEADERBOARD
    // =========================================================================
    leaderboardTimeframe: 'WEEKLY',

    async loadLeaderboard(timeframe) {
        if (timeframe) {
            this.leaderboardTimeframe = timeframe;
        } else {
            timeframe = this.leaderboardTimeframe || 'WEEKLY';
        }

        // Update active chip UI
        ['WEEKLY', 'ALL_TIME', 'STREAK'].forEach(tf => {
            const chip = document.getElementById(`timeframe-chip-${tf}`);
            if (chip) {
                if (tf === timeframe) {
                    chip.classList.add('active');
                } else {
                    chip.classList.remove('active');
                }
            }
        });

        try {
            const data = await API.getLeaderboard(timeframe, this.currentUserId, true);
            if (data) {
                this.renderLeaderboard(data);
            }
            // Ensure gamification HUD & profile stay updated in lockstep
            this.syncUserGamification();
        } catch (err) {
            console.error('Failed to load leaderboard:', err);
        }
    },

    switchLeaderboardTab(timeframe) {
        this.loadLeaderboard(timeframe);
    },

    renderLeaderboard(data) {
        if (!data) return;

        // 1. Header meta & countdown
        const countdownEl = document.getElementById('leaderboard-countdown-text');
        if (countdownEl) {
            countdownEl.textContent = data.resetCountdown || 'Resets Sunday midnight';
        }

        const totalCountEl = document.getElementById('leaderboard-total-count');
        if (totalCountEl) {
            const count = data.totalLearners || (data.entries ? data.entries.length : 0);
            totalCountEl.textContent = `${count} learners competing`;
        }

        // 2. Podium Section (Rank 2 left, Rank 1 center, Rank 3 right)
        const podiumContainer = document.getElementById('leaderboard-podium-section');
        if (podiumContainer) {
            const entries = data.entries || [];
            const rank1 = entries.find(e => e.rank === 1);
            const rank2 = entries.find(e => e.rank === 2);
            const rank3 = entries.find(e => e.rank === 3);

            const renderPodiumCol = (entry, rankNum) => {
                if (!entry) return '<div class="podium-column empty"></div>';

                const isSelf = entry.isCurrentUser || entry.currentUser || entry.userId === this.currentUserId;
                const youPill = isSelf ? '<span class="you-badge">YOU</span>' : '';
                const displayName = entry.name || entry.userName || 'Learner';
                const avatar = entry.avatar || `https://api.dicebear.com/7.x/bottts/svg?seed=${encodeURIComponent(displayName)}`;
                const crown = rankNum === 1 ? '<div class="podium-crown-icon">👑</div>' : '';
                const medalEmoji = rankNum === 1 ? '🥇' : (rankNum === 2 ? '🥈' : '🥉');
                const xpDisplay = (data.timeframe === 'ALL_TIME' ? (entry.totalXp || 0) : (entry.weeklyXp || 0)).toLocaleString();

                return `
                    <div class="podium-column podium-rank-${rankNum}">
                        <div class="podium-user-card">
                            ${crown}
                            <div class="podium-avatar-wrapper">
                                <img src="${avatar}" alt="${this.escapeHtml(displayName)}" class="podium-avatar-img">
                                <div class="podium-rank-badge">${medalEmoji}</div>
                            </div>
                            <div class="podium-user-name">
                                <span>${this.escapeHtml(displayName)}</span>
                                ${youPill}
                            </div>
                            <div class="podium-user-role">${this.escapeHtml(entry.currentRoleTitle || 'Engineer')}</div>
                            <div class="podium-xp-counter">${xpDisplay} XP</div>
                        </div>
                        <div class="podium-pedestal">
                            <span class="pedestal-rank-num">#${rankNum}</span>
                            <span class="pedestal-label">${rankNum === 1 ? 'CHAMPION' : (rankNum === 2 ? 'RUNNER UP' : '3RD PLACE')}</span>
                        </div>
                    </div>
                `;
            };

            let podiumHtml = '';
            // Olympic podium order: 2nd on left, 1st in center (tallest), 3rd on right
            if (rank2) podiumHtml += renderPodiumCol(rank2, 2);
            if (rank1) podiumHtml += renderPodiumCol(rank1, 1);
            if (rank3) podiumHtml += renderPodiumCol(rank3, 3);

            podiumContainer.innerHTML = podiumHtml;
        }

        // 3. Personalized Standing Banner
        const standingCard = document.getElementById('leaderboard-your-standing');
        if (standingCard) {
            const user = data.currentUserEntry || data.activeUserEntry;
            if (user) {
                standingCard.style.display = 'flex';
                const userRank = data.currentUserRank || data.activeUserRank || user.rank;
                const userName = user.name || user.userName || 'Learner';
                const userXpNum = data.timeframe === 'ALL_TIME' ? (user.totalXp || 0) : (user.weeklyXp || 0);
                const userXp = userXpNum.toLocaleString();

                const entries = data.entries || [];
                const nextHigherEntry = entries.find(e => e.rank === userRank - 1);
                const rank1Entry = entries.find(e => e.rank === 1);
                const getEntryXp = (e) => (data.timeframe === 'ALL_TIME' ? (e?.totalXp || 0) : (e?.weeklyXp || 0));

                let gapToNext = data.gapToNextRankXp;
                if ((!gapToNext || gapToNext <= 0) && nextHigherEntry) {
                    gapToNext = Math.max(1, getEntryXp(nextHigherEntry) - userXpNum);
                }
                if (!gapToNext || gapToNext <= 0) gapToNext = 120;

                let gapToFirst = data.gapToFirstXp;
                if ((!gapToFirst || gapToFirst <= 0) && rank1Entry) {
                    gapToFirst = Math.max(1, getEntryXp(rank1Entry) - userXpNum);
                }
                if (!gapToFirst || gapToFirst <= 0) gapToFirst = 350;

                let titleText = '';
                let descText = '';
                let icon = '⚡';

                if (userRank === 1) {
                    icon = '👑';
                    titleText = `Incredible work, ${this.escapeHtml(userName)}! You're #1 on the leaderboard!`;
                    descText = `You are holding 1st place with ${userXp} XP. Keep up the tremendous pace!`;
                } else if (userRank === 2) {
                    icon = '🥈';
                    titleText = `You're in 2nd place (#2) with ${userXp} XP!`;
                    descText = `Only <span class="standing-gap-highlight">${gapToFirst.toLocaleString()} XP</span> needed to catch #1! Complete a module lesson or lab to claim the lead.`;
                } else if (userRank === 3) {
                    icon = '🥉';
                    titleText = `You're on the podium (#3) with ${userXp} XP!`;
                    descText = `Only <span class="standing-gap-highlight">${gapToNext.toLocaleString()} XP</span> to overtake 2nd place! Complete your daily goals to climb higher.`;
                } else {
                    icon = '🚀';
                    titleText = `Your Standing: Rank #${userRank} (${userXp} XP)`;
                    descText = `Just <span class="standing-gap-highlight">${gapToNext.toLocaleString()} XP</span> to reach Rank #${userRank - 1}. Keep learning to break into the Top 3!`;
                }

                standingCard.innerHTML = `
                    <div class="standing-left">
                        <span class="standing-badge-icon">${icon}</span>
                        <div>
                            <div class="standing-title">${titleText}</div>
                            <div class="standing-desc">${descText}</div>
                        </div>
                    </div>
                    <div class="standing-right">
                        <button class="btn-primary" onclick="App.navigate('plan')">Continue Learning →</button>
                    </div>
                `;
            } else {
                standingCard.style.display = 'none';
            }
        }

        // 4. Full Table / Rankings List
        const listContainer = document.getElementById('leaderboard-list-container');
        if (listContainer) {
            const entries = data.entries || [];
            
            let tableHtml = `
                <div class="leaderboard-table-header">
                    <span>RANK</span>
                    <span>LEARNER</span>
                    <span>ROLE</span>
                    <span>STREAK</span>
                    <span>COURSES</span>
                    <span>${data.timeframe === 'ALL_TIME' ? 'LIFETIME XP' : 'XP EARNED'}</span>
                </div>
            `;

            entries.forEach(entry => {
                const isSelf = entry.isCurrentUser || entry.currentUser || entry.userId === this.currentUserId;
                const highlightClass = isSelf ? 'active-user-highlight' : '';
                const youPill = isSelf ? '<span class="you-badge">YOU</span>' : '';
                const displayName = entry.name || entry.userName || 'Learner';
                const email = entry.email || entry.userEmail || '';
                const avatar = entry.avatar || `https://api.dicebear.com/7.x/bottts/svg?seed=${encodeURIComponent(displayName)}`;
                
                let badgeClass = 'rank-other';
                let badgeContent = `#${entry.rank}`;
                if (entry.rank === 1) {
                    badgeClass = 'rank-gold';
                    badgeContent = '🥇';
                } else if (entry.rank === 2) {
                    badgeClass = 'rank-silver';
                    badgeContent = '🥈';
                } else if (entry.rank === 3) {
                    badgeClass = 'rank-bronze';
                    badgeContent = '🥉';
                }

                const xpVal = (data.timeframe === 'ALL_TIME' ? (entry.totalXp || 0) : (entry.weeklyXp || 0)).toLocaleString();
                const coursesCount = entry.completedCoursesCount !== undefined ? entry.completedCoursesCount : 0;

                tableHtml += `
                    <div class="leaderboard-table-row ${highlightClass}" data-user-id="${entry.userId}">
                        <div class="col-rank">
                            <div class="table-rank-badge ${badgeClass}">${badgeContent}</div>
                        </div>
                        <div class="col-learner">
                            <img src="${avatar}" alt="${this.escapeHtml(displayName)}" class="table-avatar-img">
                            <div class="table-learner-meta">
                                <div class="table-learner-name">
                                    <span>${this.escapeHtml(displayName)}</span>
                                    ${youPill}
                                </div>
                                <span class="table-learner-email">${this.escapeHtml(email)}</span>
                            </div>
                        </div>
                        <div class="col-role">${this.escapeHtml(entry.currentRoleTitle || 'Engineer')}</div>
                        <div class="col-streak">🔥 ${entry.streakDays || 0}d</div>
                        <div class="col-courses">🏆 ${coursesCount} finished</div>
                        <div class="col-xp">
                            <span>+${xpVal}</span>
                            <span class="col-xp-label">XP</span>
                        </div>
                    </div>
                `;
            });

            listContainer.innerHTML = tableHtml;
        }
    },

    // =========================================================================
    // QUIZ ARENA & DAILY CHALLENGES
    // =========================================================================
    async loadQuizArena() {
        try {
            await this.syncUserGamification(true);
        } catch (e) {}

        // Update streak & shield display in Quiz Arena header
        const streakCountEl = document.getElementById('arena-streak-count');
        const shieldCountEl = document.getElementById('arena-shield-count');
        if (streakCountEl && this.currentUser) {
            streakCountEl.textContent = `${this.currentUser.streakDays || 1} Day Streak`;
        }
        if (shieldCountEl && this.currentUser) {
            const shields = this.currentUser.shieldCount !== undefined ? this.currentUser.shieldCount : 1;
            shieldCountEl.textContent = `🛡️ ${shields} Shield${shields === 1 ? '' : 's'} Available`;
        }

        // Check if daily quiz is already completed today
        const dailyStatusEl = document.getElementById('arena-daily-status');
        const dailyBtn = document.getElementById('btn-start-daily');
        const todayStr = new Date().toISOString().split('T')[0];
        const lastActive = this.currentUser ? this.currentUser.lastActiveDate : null;
        const quests = (this.currentUser && this.currentUser.completedQuestIds) || [];
        const isDailyDone = quests.includes('QUEST_DAILY_QUIZ_1') || (lastActive === todayStr && quests.includes('QUEST_DAILY_QUIZ_1'));

        if (dailyStatusEl) {
            if (isDailyDone) {
                dailyStatusEl.innerHTML = '<span style="color: #10b981; font-weight: 600;">✓ Completed Today (+75 XP Claimed)</span>';
                if (dailyBtn) {
                    dailyBtn.innerHTML = '<span>Practice Daily Again</span>';
                }
            } else {
                dailyStatusEl.textContent = 'Ready to test today\'s streak';
                if (dailyBtn) {
                    dailyBtn.innerHTML = `
                        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" style="width: 16px; height: 16px;">
                            <polygon points="5 3 19 12 5 21 5 3" />
                        </svg>
                        <span>Start Daily Quiz (+75 XP)</span>
                    `;
                }
            }
        }

        // Fetch completed courses for the course selector
        try {
            const enrolled = await API.getEnrolledCourses(this.currentUserId);
            const completed = (enrolled || []).filter(c => {
                const status = (c.status || '').toUpperCase();
                const pct = c.progressPercentage || 0;
                return status === 'COMPLETED' || pct >= 100;
            });
            this.quizState.availableCompletedCourses = completed;

            const listEl = document.getElementById('quiz-completed-courses-list');
            if (listEl) {
                if (completed.length === 0) {
                    listEl.innerHTML = `
                        <div style="grid-column: 1 / -1; padding: 1.25rem; background: var(--bg-surface-2); border-radius: 12px; border: 1px dashed var(--border-color); text-align: center;">
                            <p style="margin: 0 0 0.5rem 0; color: var(--text-secondary); font-size: 0.88rem;">
                                You haven't completed a course yet! Finish lessons in your enrolled tracks or use the <strong>Custom Topic Explorer</strong> below to quiz on any topic immediately.
                            </p>
                            <button class="btn-secondary btn-sm" onclick="App.switchQuizMode('custom')">Switch to Custom Topic Explorer →</button>
                        </div>
                    `;
                    this.quizState.selectedCourseIds = [];
                } else {
                    // Default select all completed courses
                    this.quizState.selectedCourseIds = completed.map(c => c.courseId || c.id);
                    listEl.innerHTML = completed.map(c => {
                        const id = c.courseId || c.id;
                        const title = c.courseTitle || c.title || 'Course';
                        const isSelected = this.quizState.selectedCourseIds.includes(id);
                        return `
                            <div class="course-select-chip ${isSelected ? 'selected' : ''}" onclick="App.toggleCourseSelection('${id}')" id="chip-course-${id}">
                                <div class="chip-checkbox">${isSelected ? '✓' : ''}</div>
                                <span class="chip-title">${this.escapeHtml(title)}</span>
                                <span class="chip-badge">100% DONE</span>
                            </div>
                        `;
                    }).join('');
                }
            }
        } catch (err) {
            console.error('Failed to load completed courses for Quiz Arena:', err);
        }
    },

    switchQuizMode(mode) {
        const btnCourses = document.getElementById('mode-tab-courses');
        const btnCustom = document.getElementById('mode-tab-custom');
        const panelCourses = document.getElementById('mode-panel-courses');
        const panelCustom = document.getElementById('mode-panel-custom');

        if (mode === 'courses') {
            if (btnCourses) btnCourses.classList.add('active');
            if (btnCustom) btnCustom.classList.remove('active');
            if (panelCourses) panelCourses.style.display = 'block';
            if (panelCustom) panelCustom.style.display = 'none';
        } else {
            if (btnCourses) btnCourses.classList.remove('active');
            if (btnCustom) btnCustom.classList.add('active');
            if (panelCourses) panelCourses.style.display = 'none';
            if (panelCustom) panelCustom.style.display = 'block';
        }
    },

    selectQuizTopic(topic) {
        const input = document.getElementById('quiz-custom-topic-input');
        if (input) {
            input.value = topic;
            input.focus();
        }
    },

    toggleCourseSelection(courseId) {
        const idx = this.quizState.selectedCourseIds.indexOf(courseId);
        const chip = document.getElementById(`chip-course-${courseId}`);
        if (idx >= 0) {
            this.quizState.selectedCourseIds.splice(idx, 1);
            if (chip) {
                chip.classList.remove('selected');
                const box = chip.querySelector('.chip-checkbox');
                if (box) box.textContent = '';
            }
        } else {
            this.quizState.selectedCourseIds.push(courseId);
            if (chip) {
                chip.classList.add('selected');
                const box = chip.querySelector('.chip-checkbox');
                if (box) box.textContent = '✓';
            }
        }
    },

    async startDailyQuiz() {
        const btn = document.getElementById('btn-start-daily');
        const originalText = btn ? btn.innerHTML : '';
        if (btn) btn.innerHTML = '<span>⚡ Loading Daily Quiz...</span>';

        try {
            const quiz = await API.getDailyQuiz(this.currentUserId);
            this.startQuizSession(quiz, true);
        } catch (err) {
            console.error('Failed to start daily quiz:', err);
            alert('Could not start daily quiz: ' + err.message);
        } finally {
            if (btn) btn.innerHTML = originalText;
        }
    },

    async generateCompletedCoursesQuiz() {
        let courseIds = this.quizState.selectedCourseIds;
        if (!courseIds || courseIds.length === 0) {
            if (this.quizState.availableCompletedCourses && this.quizState.availableCompletedCourses.length > 0) {
                courseIds = this.quizState.availableCompletedCourses.map(c => c.courseId || c.id);
                this.quizState.selectedCourseIds = courseIds;
            } else {
                courseIds = ['PLAN_ADE_01', 'PLAN_K8S_01'];
            }
        }

        const countEl = document.getElementById('quiz-courses-count');
        const diffEl = document.getElementById('quiz-courses-difficulty');
        const questionCount = countEl ? parseInt(countEl.value, 10) : 5;
        const difficultyLevel = diffEl ? diffEl.value : 'INTERMEDIATE';

        const btn = document.getElementById('btn-gen-courses-quiz');
        const originalHtml = btn ? btn.innerHTML : '';
        if (btn) btn.innerHTML = '<span>✨ Generating AI Quiz...</span>';

        try {
            const quiz = await API.generateQuiz({
                mode: 'COMPLETED_COURSES',
                courseIds: courseIds,
                questionCount: questionCount,
                difficultyLevel: difficultyLevel,
                userId: this.currentUserId
            });
            this.startQuizSession(quiz, false);
        } catch (err) {
            console.error('Quiz generation error:', err);
            alert('Quiz generation failed: ' + err.message);
        } finally {
            if (btn) btn.innerHTML = originalHtml;
        }
    },

    async generateCustomTopicQuiz() {
        const input = document.getElementById('quiz-custom-topic-input');
        const topic = (input && input.value.trim()) ? input.value.trim() : 'Software Architecture & Best Practices';

        const countEl = document.getElementById('quiz-custom-count');
        const diffEl = document.getElementById('quiz-custom-difficulty');
        const questionCount = countEl ? parseInt(countEl.value, 10) : 5;
        const difficultyLevel = diffEl ? diffEl.value : 'INTERMEDIATE';

        const btn = document.getElementById('btn-gen-custom-quiz');
        const originalHtml = btn ? btn.innerHTML : '';
        if (btn) btn.innerHTML = '<span>✨ Generating AI Quiz...</span>';

        try {
            const quiz = await API.generateQuiz({
                mode: 'CUSTOM_TOPIC',
                customTopic: topic,
                questionCount: questionCount,
                difficultyLevel: difficultyLevel,
                userId: this.currentUserId
            });
            this.startQuizSession(quiz, false);
        } catch (err) {
            console.error('Quiz generation error:', err);
            alert('Quiz generation failed: ' + err.message);
        } finally {
            if (btn) btn.innerHTML = originalHtml;
        }
    },

    startQuizSession(quiz, isDaily) {
        if (!quiz || !quiz.questions || quiz.questions.length === 0) {
            alert('Quiz contains no questions.');
            return;
        }

        this.quizState.activeQuiz = quiz;
        this.quizState.isDaily = !!isDaily;
        this.quizState.currentQuestionIndex = 0;
        this.quizState.userAnswers = {};

        // Hide deck, hero, results; reveal player
        const deck = document.getElementById('arena-config-deck');
        const hero = document.getElementById('arena-daily-hero');
        const results = document.getElementById('arena-results');
        const player = document.getElementById('arena-player');

        if (deck) deck.style.display = 'none';
        if (hero) hero.style.display = 'none';
        if (results) results.style.display = 'none';
        if (player) {
            player.style.display = 'block';
            player.scrollIntoView({ behavior: 'smooth' });
        }

        // Header info
        const typeBadge = document.getElementById('player-quiz-type-badge');
        const titleEl = document.getElementById('player-quiz-title');
        const xpPot = document.getElementById('player-xp-pot');

        if (typeBadge) {
            typeBadge.textContent = isDaily ? 'DAILY SPRINT' : (quiz.quizType || 'MASTERY CHECK');
        }
        if (titleEl) {
            titleEl.textContent = quiz.title || 'Knowledge Assessment';
        }
        if (xpPot) {
            xpPot.textContent = `+${quiz.totalXpReward || (quiz.questions.length * 25)} XP Potential`;
        }

        this.renderCurrentQuizQuestion();
    },

    renderCurrentQuizQuestion() {
        const quiz = this.quizState.activeQuiz;
        if (!quiz || !quiz.questions) return;

        const total = quiz.questions.length;
        const idx = this.quizState.currentQuestionIndex;
        const q = quiz.questions[idx];

        // Progress Text & Fill
        const progText = document.getElementById('player-progress-text');
        const progFill = document.getElementById('player-progress-fill');
        const runningScore = document.getElementById('player-running-score');

        if (progText) progText.textContent = `Question ${idx + 1} of ${total}`;
        if (progFill) progFill.style.width = `${((idx + 1) / total) * 100}%`;

        // Calculate answered count
        const answeredCount = Object.keys(this.quizState.userAnswers).length;
        if (runningScore) runningScore.textContent = `Answered: ${answeredCount}/${total}`;

        // Question Text
        const qTextEl = document.getElementById('player-question-text');
        if (qTextEl) qTextEl.textContent = q.questionText || '';

        // Options Grid
        const optionsGrid = document.getElementById('player-options-grid');
        const userChoice = this.quizState.userAnswers[idx];
        const hasAnswered = userChoice !== undefined;

        if (optionsGrid) {
            const letterBadges = ['A', 'B', 'C', 'D'];
            optionsGrid.innerHTML = (q.options || []).map((opt, optIdx) => {
                let stateClass = '';
                if (hasAnswered) {
                    if (optIdx === q.correctOptionIndex) {
                        stateClass = 'correct';
                    } else if (optIdx === userChoice) {
                        stateClass = 'wrong';
                    }
                    stateClass += ' locked';
                }

                return `
                    <button type="button" class="quiz-option-tile ${stateClass}" onclick="App.selectQuizOption(${optIdx})" ${hasAnswered ? 'disabled' : ''}>
                        <div class="option-badge">${letterBadges[optIdx] || optIdx + 1}</div>
                        <span class="option-text">${this.escapeHtml(opt)}</span>
                    </button>
                `;
            }).join('');
        }

        // Explanation Banner
        const explBanner = document.getElementById('player-explanation-banner');
        const explStatus = document.getElementById('explanation-status');
        const explBody = document.getElementById('explanation-body');
        const explIcon = document.getElementById('explanation-icon');

        if (explBanner && explStatus && explBody) {
            if (hasAnswered) {
                const isCorrect = userChoice === q.correctOptionIndex;
                explBanner.className = `player-explanation-banner ${isCorrect ? 'correct' : 'wrong'}`;
                explBanner.style.display = 'flex';
                explStatus.textContent = isCorrect ? '✓ Correct Answer!' : '✕ Incorrect';
                explIcon.textContent = isCorrect ? '🎉' : '💡';
                explBody.textContent = q.explanation || 'Review the core concepts behind this architecture pattern.';
            } else {
                explBanner.style.display = 'none';
            }
        }

        // Prev & Next Buttons
        const prevBtn = document.getElementById('player-btn-prev');
        const nextBtn = document.getElementById('player-btn-next');

        if (prevBtn) {
            prevBtn.disabled = (idx === 0);
        }

        if (nextBtn) {
            if (idx === total - 1) {
                nextBtn.innerHTML = '<span>Submit & Complete 🚀</span>';
                nextBtn.className = 'btn-primary btn-completion-glow';
            } else {
                nextBtn.innerHTML = '<span>Next Question →</span>';
                nextBtn.className = 'btn-primary';
            }
        }
    },

    selectQuizOption(optionIndex) {
        const idx = this.quizState.currentQuestionIndex;
        if (this.quizState.userAnswers[idx] !== undefined) return; // already answered

        this.quizState.userAnswers[idx] = optionIndex;
        this.renderCurrentQuizQuestion();
    },

    prevQuizQuestion() {
        if (this.quizState.currentQuestionIndex > 0) {
            this.quizState.currentQuestionIndex--;
            this.renderCurrentQuizQuestion();
        }
    },

    nextQuizQuestion() {
        const total = this.quizState.activeQuiz ? this.quizState.activeQuiz.questions.length : 0;
        if (this.quizState.currentQuestionIndex < total - 1) {
            this.quizState.currentQuestionIndex++;
            this.renderCurrentQuizQuestion();
        } else {
            // Last question -> Submit
            this.submitQuiz();
        }
    },

    quitQuiz() {
        if (confirm('Are you sure you want to exit? Any uncompleted quiz progress will not be saved.')) {
            this.resetQuizArena();
        }
    },

    async submitQuiz() {
        const quiz = this.quizState.activeQuiz;
        if (!quiz) return;

        const nextBtn = document.getElementById('player-btn-next');
        if (nextBtn) nextBtn.textContent = 'Submitting & Evaluating...';

        try {
            const payload = {
                quizId: quiz.id,
                userId: this.currentUserId,
                isDaily: this.quizState.isDaily,
                answers: this.quizState.userAnswers,
                totalQuestions: quiz.questions.length,
                questions: quiz.questions
            };

            const result = await API.submitQuiz(payload);
            await this.syncUserGamification(true);

            // Hide player, show celebration results
            const player = document.getElementById('arena-player');
            const results = document.getElementById('arena-results');
            if (player) player.style.display = 'none';
            if (results) {
                results.style.display = 'block';
                results.scrollIntoView({ behavior: 'smooth' });
            }

            try {
                this.renderQuizResults(result);
            } catch (renderErr) {
                console.error('Render quiz results error:', renderErr);
            }

        } catch (err) {
            console.error('Quiz submission error:', err);
            alert('Failed to submit quiz: ' + err.message);
            if (nextBtn) nextBtn.textContent = 'Submit & Complete 🚀';
        }
    },

    renderQuizResults(result) {
        const badgeEmoji = document.getElementById('results-badge-emoji');
        const kicker = document.getElementById('results-kicker');
        const title = document.getElementById('results-title');
        const desc = document.getElementById('results-desc');

        const scorePct = document.getElementById('results-score-pct');
        const scoreRatio = document.getElementById('results-score-ratio');
        const xpEarned = document.getElementById('results-xp-earned');
        const streakVal = document.getElementById('results-streak-val');
        const streakStatus = document.getElementById('results-streak-status');
        const passStatus = document.getElementById('results-pass-status');

        const isPassed = result.passed;
        if (badgeEmoji) badgeEmoji.textContent = isPassed ? '🏆' : '📚';
        if (kicker) kicker.textContent = isPassed ? 'MASTERY VERIFIED' : 'PRACTICE SPRINT';
        if (title) title.textContent = isPassed ? 'Outstanding Knowledge Mastery!' : 'Good Effort! Keep Reviewing!';
        if (desc) {
            desc.textContent = isPassed
                ? 'Your passing score has awarded you full XP and advanced your learning progression.'
                : 'Review the detailed explanations below to master the architectural trade-offs.';
        }

        if (scorePct) scorePct.textContent = `${result.scorePercentage}%`;
        if (scoreRatio) scoreRatio.textContent = `${result.correctCount} / ${result.totalQuestions} Correct`;
        if (xpEarned) xpEarned.textContent = `+${result.totalXpEarned} XP`;

        if (streakVal) {
            streakVal.textContent = `🔥 ${result.newStreakDays} Days`;
        }
        if (streakStatus) {
            streakStatus.textContent = result.streakIncremented ? 'Extended today!' : 'Active streak maintained';
        }

        if (passStatus) {
            passStatus.textContent = isPassed ? 'PASSED ✓' : 'NEEDS PRACTICE';
            passStatus.className = isPassed ? 'result-stat-val text-success' : 'result-stat-val text-warning';
        }

        // New badge notification
        const badgeBanner = document.getElementById('results-badge-banner');
        const badgeText = document.getElementById('results-unlocked-badges-text');
        if (badgeBanner && badgeText) {
            if (result.newBadgesUnlocked && result.newBadgesUnlocked.length > 0) {
                badgeBanner.style.display = 'flex';
                badgeText.textContent = result.newBadgesUnlocked.join(', ');
            } else {
                badgeBanner.style.display = 'none';
            }
        }

        // Render detailed reviews
        const reviewList = document.getElementById('results-review-list');
        if (reviewList && result.reviews) {
            const letters = ['A', 'B', 'C', 'D'];
            reviewList.innerHTML = result.reviews.map((r, i) => {
                const userAnsText = (r.selectedOptionIndex >= 0 && r.options)
                    ? `${letters[r.selectedOptionIndex]}. ${r.options[r.selectedOptionIndex]}`
                    : 'No answer selected';
                const correctAnsText = (r.correctOptionIndex >= 0 && r.options)
                    ? `${letters[r.correctOptionIndex]}. ${r.options[r.correctOptionIndex]}`
                    : '';

                return `
                    <div class="review-card ${r.correct ? 'correct' : 'wrong'}">
                        <div class="review-q-head">
                            <span class="review-q-num">Question ${i + 1}</span>
                            <span class="review-q-badge ${r.correct ? 'correct' : 'wrong'}">
                                ${r.correct ? '✓ Correct' : '✕ Incorrect'}
                            </span>
                        </div>
                        <h4 class="review-q-text">${this.escapeHtml(r.questionText)}</h4>
                        <div class="review-q-ans-box">
                            <div class="review-user-ans">Your answer: <strong>${this.escapeHtml(userAnsText)}</strong></div>
                            ${!r.correct ? `<div class="review-correct-ans">Correct answer: <strong>${this.escapeHtml(correctAnsText)}</strong></div>` : ''}
                        </div>
                        <div class="review-expl-box">
                            💡 <strong>Rationale:</strong> ${this.escapeHtml(r.explanation || 'Understanding this pattern ensures resilient system behavior.')}
                        </div>
                    </div>
                `;
            }).join('');
        }

        // Trigger celebratory confetti on high scores
        if (result.scorePercentage >= 80) {
            try {
                this.startConfetti('quiz-confetti-canvas');
            } catch (confettiErr) {
                console.warn('Confetti animation notice:', confettiErr);
            }
        }
    },

    resetQuizArena() {
        this.quizState.activeQuiz = null;
        this.quizState.currentQuestionIndex = 0;
        this.quizState.userAnswers = {};

        const deck = document.getElementById('arena-config-deck');
        const hero = document.getElementById('arena-daily-hero');
        const player = document.getElementById('arena-player');
        const results = document.getElementById('arena-results');

        if (deck) deck.style.display = 'block';
        if (hero) hero.style.display = 'flex';
        if (player) player.style.display = 'none';
        if (results) results.style.display = 'none';

        this.loadQuizArena();
    },

    // =========================================================================
    // VIEW 10: COURSE DISCUSSIONS & AI MENTOR FORUM
    // =========================================================================
    async loadDiscussions(forceRefresh = false) {
        const feedEl = document.getElementById('discussions-feed');
        if (!feedEl) return;

        this.populateDiscussionCoursesDropdown();

        feedEl.innerHTML = `
            <div class="discussions-loading">
                <div class="spinner-inline"></div>
                <span>Loading course discussions...</span>
            </div>
        `;

        try {
            const threads = await API.getDiscussions({
                courseId: this.discussionState.selectedCourseId,
                status: this.discussionState.selectedStatus,
                search: this.discussionState.searchQuery,
                sort: this.discussionState.selectedSort,
                userId: this.currentUserId
            }, forceRefresh);

            this.discussionState.threads = threads || [];
            this.renderDiscussionThreads();
        } catch (err) {
            console.error('Error loading discussions:', err);
            feedEl.innerHTML = `
                <div class="discussion-empty-state">
                    <div class="empty-state-icon">⚠️</div>
                    <h3 class="empty-state-title">Unable to Load Discussions</h3>
                    <p class="empty-state-desc">There was an issue retrieving discussion questions. Please try again.</p>
                    <button class="btn-primary" onclick="App.loadDiscussions(true)">Retry</button>
                </div>
            `;
        }
    },

    populateDiscussionCoursesDropdown() {
        const filterSelect = document.getElementById('discussion-course-filter');
        const askCourseSelect = document.getElementById('ask-course-select');

        if (!this.courses || this.courses.length === 0) {
            API.getCourses().then(courses => {
                if (courses && courses.length > 0) {
                    this.courses = courses;
                    this.populateDiscussionCoursesDropdown();
                }
            }).catch(e => console.warn('Could not fetch courses for discussion dropdown:', e));
            return;
        }

        if (filterSelect && filterSelect.options.length <= 1) {
            this.courses.forEach(c => {
                const opt = document.createElement('option');
                opt.value = c.id;
                opt.textContent = c.title;
                filterSelect.appendChild(opt);
            });
            if (this.discussionState.selectedCourseId) {
                filterSelect.value = this.discussionState.selectedCourseId;
            }
        }

        if (askCourseSelect && askCourseSelect.options.length <= 1) {
            this.courses.forEach(c => {
                const opt = document.createElement('option');
                opt.value = c.id;
                opt.textContent = c.title;
                askCourseSelect.appendChild(opt);
            });
            if (this.discussionState.selectedCourseId) {
                askCourseSelect.value = this.discussionState.selectedCourseId;
            } else if (this.courses.length > 0) {
                askCourseSelect.value = this.courses[0].id;
            }
        }
    },

    handleDiscussionCourseFilter(courseId) {
        this.discussionState.selectedCourseId = courseId || '';
        this.loadDiscussions();
    },

    handleDiscussionStatusFilter(status) {
        this.discussionState.selectedStatus = status || 'ALL';
        document.querySelectorAll('.status-tab').forEach(tab => tab.classList.remove('active'));
        const activeTab = document.getElementById(`tab-status-${status}`);
        if (activeTab) activeTab.classList.add('active');
        this.loadDiscussions();
    },

    handleDiscussionSortFilter(sort) {
        this.discussionState.selectedSort = sort || 'NEWEST';
        this.loadDiscussions();
    },

    debounceDiscussionSearch() {
        const input = document.getElementById('discussion-search-input');
        const clearBtn = document.getElementById('btn-clear-discussion-search');
        if (!input) return;

        const val = input.value.trim();
        if (clearBtn) clearBtn.style.display = val ? 'block' : 'none';

        if (this.discussionState.searchDebounceTimer) {
            clearTimeout(this.discussionState.searchDebounceTimer);
        }

        this.discussionState.searchDebounceTimer = setTimeout(() => {
            this.discussionState.searchQuery = val;
            this.loadDiscussions();
        }, 300);
    },

    clearDiscussionSearch() {
        const input = document.getElementById('discussion-search-input');
        const clearBtn = document.getElementById('btn-clear-discussion-search');
        if (input) input.value = '';
        if (clearBtn) clearBtn.style.display = 'none';
        this.discussionState.searchQuery = '';
        this.loadDiscussions();
    },

    renderDiscussionThreads() {
        const feedEl = document.getElementById('discussions-feed');
        if (!feedEl) return;

        const threads = this.discussionState.threads;
        if (!threads || threads.length === 0) {
            feedEl.innerHTML = `
                <div class="discussion-empty-state">
                    <div class="empty-state-icon">💬</div>
                    <h3 class="empty-state-title">No Discussion Questions Found</h3>
                    <p class="empty-state-desc">
                        ${this.discussionState.searchQuery || this.discussionState.selectedCourseId || this.discussionState.selectedStatus !== 'ALL'
                            ? 'No questions matched your current filters. Try changing or clearing your search criteria.'
                            : 'Be the first to post a question for your courses! You will earn +15 XP.'}
                    </p>
                    <button class="btn-primary" onclick="App.openAskQuestionModal()">
                        <span>+ Ask Question (+15 XP)</span>
                    </button>
                </div>
            `;
            return;
        }

        feedEl.innerHTML = threads.map(t => {
            const isSolved = t.resolved;
            const hasAi = t.hasAiAnswer;
            const isUpvoted = t.upvotedByCurrentUser;
            const timeAgo = this.formatTimeAgo(t.createdAt);

            const tagsHtml = (t.tags || []).map(tag => `<span class="tag-item">#${this.escapeHtml(tag)}</span>`).join('');

            return `
                <div class="thread-card ${isSolved ? 'is-solved' : ''}" onclick="App.openThreadDetail('${t.id}')">
                    <div class="vote-widget ${isUpvoted ? 'voted' : ''}" onclick="App.handleToggleThreadVote(event, '${t.id}')" title="Upvote question">
                        <svg class="vote-icon" viewBox="0 0 24 24" fill="${isUpvoted ? 'currentColor' : 'none'}" stroke="currentColor" stroke-width="2.5">
                            <polyline points="18 15 12 9 6 15" />
                        </svg>
                        <span class="vote-count" id="vote-count-${t.id}">${t.upvotes || 0}</span>
                    </div>

                    <div class="thread-card-main">
                        <div class="thread-card-badges">
                            <span class="badge-course-pill" title="${this.escapeHtml(t.courseTitle)}">${this.escapeHtml(t.courseTitle)}</span>
                            ${isSolved ? `<span class="badge-status-solved">✓ Solved</span>` : ''}
                            ${hasAi ? `<span class="badge-status-ai">⚡ AI Answer</span>` : ''}
                        </div>

                        <h3 class="thread-card-title">${this.escapeHtml(t.title)}</h3>
                        <p class="thread-card-snippet">${this.escapeHtml(t.contentSnippet)}</p>

                        <div class="thread-card-footer">
                            <div class="thread-author-meta">
                                ${this.renderAuthorAvatar(t.authorAvatar, t.authorName)}
                                <span class="author-name-text">${this.escapeHtml(t.authorName || 'Learner')}</span>
                                <span class="author-role-sub">· ${this.escapeHtml(t.authorRole || 'Community Member')}</span>
                                <span>· ${timeAgo}</span>
                            </div>

                            <span class="thread-reply-pill">
                                💬 ${t.replyCount || 0} ${(t.replyCount === 1 ? 'reply' : 'replies')}
                            </span>
                        </div>
                    </div>
                </div>
            `;
        }).join('');
    },

    async openThreadDetail(threadId) {
        const listView = document.getElementById('discussions-list-view');
        const detailView = document.getElementById('thread-detail-view');
        const detailContent = document.getElementById('thread-detail-content');

        if (!listView || !detailView || !detailContent) return;

        listView.style.display = 'none';
        detailView.style.display = 'block';

        detailContent.innerHTML = `
            <div class="discussions-loading">
                <div class="spinner-inline"></div>
                <span>Loading question details...</span>
            </div>
        `;

        try {
            const thread = await API.getDiscussionDetail(threadId, this.currentUserId, true);
            this.discussionState.activeThread = thread;
            this.renderThreadDetail(thread);
        } catch (err) {
            console.error('Error loading thread detail:', err);
            detailContent.innerHTML = `
                <div class="discussion-empty-state">
                    <div class="empty-state-icon">⚠️</div>
                    <h3 class="empty-state-title">Could Not Load Question</h3>
                    <p class="empty-state-desc">This thread may have been removed or is temporarily unavailable.</p>
                    <button class="btn-primary" onclick="App.closeThreadDetail()">Back to Discussions</button>
                </div>
            `;
        }
    },

    closeThreadDetail() {
        const listView = document.getElementById('discussions-list-view');
        const detailView = document.getElementById('thread-detail-view');
        if (listView && detailView) {
            detailView.style.display = 'none';
            listView.style.display = 'block';
        }
        this.discussionState.activeThread = null;
        this.loadDiscussions(true);
    },

    renderThreadDetail(thread) {
        const detailContent = document.getElementById('thread-detail-content');
        if (!detailContent) return;

        const isUpvoted = thread.upvotedByCurrentUser;
        const timeAgo = this.formatTimeAgo(thread.createdAt);
        const tagsHtml = (thread.tags || []).map(tag => `<span class="tag-item">#${this.escapeHtml(tag)}</span>`).join('');
        const replies = thread.replies || [];
        const isAuthor = thread.authorId === this.currentUserId;

        detailContent.innerHTML = `
            <!-- Main Question Card -->
            <div class="thread-main-card">
                <div class="thread-main-header">
                    <div class="vote-widget ${isUpvoted ? 'voted' : ''}" onclick="App.handleToggleThreadVote(event, '${thread.id}', true)">
                        <svg class="vote-icon" viewBox="0 0 24 24" fill="${isUpvoted ? 'currentColor' : 'none'}" stroke="currentColor" stroke-width="2.5">
                            <polyline points="18 15 12 9 6 15" />
                        </svg>
                        <span class="vote-count" id="detail-vote-count-${thread.id}">${thread.upvotes || 0}</span>
                    </div>

                    <div style="flex: 1; min-width: 0;">
                        <div class="thread-card-badges">
                            <span class="badge-course-pill">${this.escapeHtml(thread.courseTitle)}</span>
                            ${thread.lessonTitle ? `<span class="tag-item" style="color:#94a3b8;">📖 ${this.escapeHtml(thread.lessonTitle)}</span>` : ''}
                            ${thread.resolved ? `<span class="badge-status-solved">✓ Solved</span>` : ''}
                            ${thread.hasAiAnswer ? `<span class="badge-status-ai">🤖 AI Mentor Answer</span>` : ''}
                        </div>
                        <h2 class="thread-main-title">${this.escapeHtml(thread.title)}</h2>
                        <div class="thread-author-meta">
                            ${this.renderAuthorAvatar(thread.authorAvatar, thread.authorName)}
                            <span class="author-name-text">${this.escapeHtml(thread.authorName || 'Learner')}</span>
                            <span class="author-role-sub">(${this.escapeHtml(thread.authorRole || 'Community Member')})</span>
                            <span>· Asked ${timeAgo}</span>
                        </div>
                    </div>
                </div>

                <div class="thread-main-body">
                    ${this.renderMarkdown(thread.content)}
                </div>

                ${tagsHtml ? `<div class="thread-card-tags" style="margin-bottom: 1rem;">${tagsHtml}</div>` : ''}

                <div class="thread-main-actions">
                    <span style="font-size: 0.85rem; color: var(--text-secondary);">
                        💡 Contributing an answer awards <strong>+25 XP</strong>
                    </span>
                    ${(thread.hasAiAnswer || replies.some(r => r.aiGenerated)) ? `
                        <div class="ai-answered-status-group">
                            <span class="badge-ai-answered-pill">
                                <span>✓</span> AI Mentor Answered
                            </span>
                            <button class="btn-ask-ai-mentor btn-ask-ai-mentor-sm" id="btn-request-ai-answer" onclick="App.handleRequestAiAnswer('${thread.id}')" title="Request an updated breakdown from CareerPulse AI Mentor">
                                <span>🔄 Re-analyze with AI</span>
                            </button>
                        </div>
                    ` : `
                        <button class="btn-ask-ai-mentor" id="btn-request-ai-answer" onclick="App.handleRequestAiAnswer('${thread.id}')">
                            <span>⚡ Ask AI Mentor to Answer</span>
                        </button>
                    `}
                </div>
            </div>

            <!-- Replies Section -->
            <div class="replies-section-header">
                <h3 class="replies-section-title">
                    Community & AI Answers (${replies.length})
                </h3>
            </div>

            <div class="replies-list" id="replies-list-container">
                ${replies.length === 0 ? `
                    <div class="discussion-empty-state" style="padding: 2rem;">
                        <div class="empty-state-icon">💡</div>
                        <h4 class="empty-state-title" style="font-size: 1rem;">No answers yet</h4>
                        <p class="empty-state-desc" style="font-size: 0.84rem;">Be the first to help out or click "Ask AI Mentor to Answer" above!</p>
                    </div>
                ` : replies.map(r => {
                    const rUpvoted = r.upvotedByCurrentUser;
                    const rTimeAgo = this.formatTimeAgo(r.createdAt);
                    const canAccept = isAuthor && !thread.resolved && !r.acceptedSolution;

                    return `
                        <div class="reply-card ${r.aiGenerated ? 'ai-reply' : ''} ${r.acceptedSolution ? 'accepted-solution' : ''}" id="reply-card-${r.id}">
                            <div class="vote-widget ${rUpvoted ? 'voted' : ''}" onclick="App.handleToggleReplyVote(event, '${thread.id}', '${r.id}')" title="Upvote answer">
                                <svg class="vote-icon" viewBox="0 0 24 24" fill="${rUpvoted ? 'currentColor' : 'none'}" stroke="currentColor" stroke-width="2.5">
                                    <polyline points="18 15 12 9 6 15" />
                                </svg>
                                <span class="vote-count" id="reply-vote-count-${r.id}">${r.upvotes || 0}</span>
                            </div>

                            <div class="reply-card-main">
                                <div class="reply-header-row">
                                    <div class="reply-author-info">
                                        ${this.renderAuthorAvatar(r.authorAvatar, r.authorName, 'author-avatar-chip')}
                                        <div>
                                            <span class="author-name-text">${this.escapeHtml(r.authorName)}</span>
                                            <span class="author-role-sub">· ${this.escapeHtml(r.authorRole || 'Contributor')}</span>
                                            <span style="font-size: 0.75rem; color: var(--text-muted);">· ${rTimeAgo}</span>
                                        </div>
                                    </div>

                                    <div style="display: flex; align-items: center; gap: 0.5rem;">
                                        ${r.aiGenerated ? `<span class="badge-status-ai">🤖 AI Technical Mentor</span>` : ''}
                                        ${r.acceptedSolution ? `<span class="badge-status-solved">✓ Accepted Solution (+50 XP)</span>` : ''}
                                    </div>
                                </div>

                                <div class="reply-body-markdown">
                                    ${this.renderMarkdown(r.content)}
                                </div>

                                ${canAccept ? `
                                    <div class="reply-footer-row">
                                        <button class="btn-mark-solution" onclick="App.handleMarkSolution('${thread.id}', '${r.id}')">
                                            ✓ Mark as Accepted Solution (+50 XP)
                                        </button>
                                    </div>
                                ` : ''}
                            </div>
                        </div>
                    `;
                }).join('')}
            </div>

            <!-- Reply Composer Card -->
            <div class="reply-composer-card">
                <div class="composer-header">
                    <h3 class="composer-title">Your Answer</h3>
                    <span class="xp-pill-badge">💡 Earn +25 XP</span>
                </div>
                <form onsubmit="App.handlePostReplySubmit(event, '${thread.id}')">
                    <textarea id="reply-composer-text" class="composer-textarea" placeholder="Share your technical solution, code snippet, or explanation to help solve this problem (Markdown supported)..." required></textarea>
                    <div class="composer-footer">
                        <span class="composer-hints">Supports **bold**, \`code\`, and \`\`\`code blocks\`\`\`</span>
                        <button type="submit" class="btn-primary" id="btn-submit-reply">
                            <span>Post Reply (+25 XP)</span>
                        </button>
                    </div>
                </form>
            </div>
        `;
    },

    async handleToggleThreadVote(e, threadId, isDetail = false) {
        if (e) e.stopPropagation();
        try {
            const res = await API.toggleDiscussionThreadUpvote(threadId, this.currentUserId);
            const countEl = document.getElementById(isDetail ? `detail-vote-count-${threadId}` : `vote-count-${threadId}`);
            if (countEl) countEl.textContent = res.upvotes;

            const target = e.currentTarget;
            if (target) {
                target.classList.toggle('voted', res.isUpvoted);
                const icon = target.querySelector('.vote-icon');
                if (icon) icon.setAttribute('fill', res.isUpvoted ? 'currentColor' : 'none');
            }
        } catch (err) {
            console.error('Error toggling thread vote:', err);
        }
    },

    async handleToggleReplyVote(e, threadId, replyId) {
        if (e) e.stopPropagation();
        try {
            const res = await API.toggleDiscussionReplyUpvote(threadId, replyId, this.currentUserId);
            const countEl = document.getElementById(`reply-vote-count-${replyId}`);
            if (countEl) countEl.textContent = res.upvotes;

            const target = e.currentTarget;
            if (target) {
                target.classList.toggle('voted', res.isUpvoted);
                const icon = target.querySelector('.vote-icon');
                if (icon) icon.setAttribute('fill', res.isUpvoted ? 'currentColor' : 'none');
            }
        } catch (err) {
            console.error('Error toggling reply vote:', err);
        }
    },

    openAskQuestionModal() {
        this.populateDiscussionCoursesDropdown();
        const modal = document.getElementById('discussion-ask-modal');
        if (modal) modal.style.display = 'flex';
        const titleInput = document.getElementById('ask-title-input');
        if (titleInput) setTimeout(() => titleInput.focus(), 80);
    },

    closeAskQuestionModal() {
        const modal = document.getElementById('discussion-ask-modal');
        if (modal) modal.style.display = 'none';
        const form = document.getElementById('form-ask-discussion');
        if (form) form.reset();
        const requestAiCb = document.getElementById('ask-request-ai');
        if (requestAiCb) requestAiCb.checked = true;
    },

    async handleCreateThreadSubmit(e) {
        if (e) e.preventDefault();
        const btn = document.getElementById('btn-submit-new-thread');
        const courseSelect = document.getElementById('ask-course-select');
        const titleInput = document.getElementById('ask-title-input');
        const contentInput = document.getElementById('ask-content-textarea');
        const requestAiCb = document.getElementById('ask-request-ai');

        if (!courseSelect || !titleInput) return;

        const courseId = courseSelect.value;
        const title = titleInput.value.trim();
        const content = contentInput && contentInput.value.trim() ? contentInput.value.trim() : title;
        const requestAiAnswer = requestAiCb ? requestAiCb.checked : true;

        if (!courseId || !title) {
            alert('Please select a course and enter your question.');
            return;
        }

        if (btn) {
            btn.disabled = true;
            btn.innerHTML = `<span>Posting Question...</span>`;
        }

        try {
            const created = await API.createDiscussionThread({
                courseId,
                lessonId,
                lessonTitle,
                title,
                content,
                tags,
                authorId: this.currentUserId,
                requestAiAnswer
            });

            this.closeAskQuestionModal();
            this.showToast('🎉 Question posted! <strong>+15 XP</strong> added to your profile.');
            this.syncUserGamification(true);

            // Open the new thread detail
            if (created && created.id) {
                this.openThreadDetail(created.id);
            } else {
                this.loadDiscussions(true);
            }
        } catch (err) {
            console.error('Error posting question:', err);
            alert('Could not post question. Please try again.');
        } finally {
            if (btn) {
                btn.disabled = false;
                btn.innerHTML = `<span>Post Question (+15 XP)</span>`;
            }
        }
    },

    async handlePostReplySubmit(e, threadId) {
        if (e) e.preventDefault();
        const textarea = document.getElementById('reply-composer-text');
        const btn = document.getElementById('btn-submit-reply');
        if (!textarea) return;

        const content = textarea.value.trim();
        if (!content) return;

        if (btn) {
            btn.disabled = true;
            btn.innerHTML = `<span>Posting...</span>`;
        }

        try {
            await API.addDiscussionReply(threadId, {
                authorId: this.currentUserId,
                content
            });

            textarea.value = '';
            this.showToast('💡 Answer posted! <strong>+25 XP</strong> awarded for your contribution.');
            this.syncUserGamification(true);

            // Re-render thread details
            const thread = await API.getDiscussionDetail(threadId, this.currentUserId, true);
            this.discussionState.activeThread = thread;
            this.renderThreadDetail(thread);
        } catch (err) {
            console.error('Error posting reply:', err);
            alert('Could not post reply. Please try again.');
        } finally {
            if (btn) {
                btn.disabled = false;
                btn.innerHTML = `<span>Post Reply (+25 XP)</span>`;
            }
        }
    },

    async handleRequestAiAnswer(threadId) {
        const btn = document.getElementById('btn-request-ai-answer');
        const wasReanalyze = btn && btn.textContent.includes('Re-analyze');
        if (btn) {
            btn.disabled = true;
            btn.innerHTML = `<div class="spinner-inline" style="width:14px;height:14px;"></div><span>AI Mentor analyzing...</span>`;
        }

        try {
            await API.requestAiDiscussionAnswer(threadId);
            this.showToast('🤖 CareerPulse AI Mentor generated an architectural answer!');

            // Reload and re-render thread detail
            const thread = await API.getDiscussionDetail(threadId, this.currentUserId, true);
            this.discussionState.activeThread = thread;
            this.renderThreadDetail(thread);
        } catch (err) {
            console.error('Error generating AI answer:', err);
            alert('Could not generate AI answer. Please try again.');
            if (btn) {
                btn.disabled = false;
                btn.innerHTML = wasReanalyze ? `<span>🔄 Re-analyze with AI</span>` : `<span>⚡ Ask AI Mentor to Answer</span>`;
            }
        }
    },

    async handleMarkSolution(threadId, replyId) {
        try {
            const thread = await API.resolveDiscussionThread(threadId, replyId, this.currentUserId);
            this.showToast('🏆 Solution accepted! <strong>+50 XP</strong> awarded to the contributor.');
            this.syncUserGamification(true);
            this.discussionState.activeThread = thread;
            this.renderThreadDetail(thread);
        } catch (err) {
            console.error('Error marking solution:', err);
            alert('Could not mark solution. Please try again.');
        }
    },

    formatTimeAgo(dateString) {
        if (!dateString) return 'recently';
        const date = new Date(dateString);
        const now = new Date();
        const diffSec = Math.floor((now - date) / 1000);
        if (diffSec < 60) return 'just now';
        const diffMin = Math.floor(diffSec / 60);
        if (diffMin < 60) return `${diffMin}m ago`;
        const diffHour = Math.floor(diffMin / 60);
        if (diffHour < 24) return `${diffHour}h ago`;
        const diffDay = Math.floor(diffHour / 24);
        if (diffDay < 30) return `${diffDay}d ago`;
        return date.toLocaleDateString();
    },

    showToast(message, type = 'success') {
        let container = document.getElementById('global-toast-container');
        if (!container) {
            container = document.createElement('div');
            container.id = 'global-toast-container';
            container.style.cssText = 'position:fixed;bottom:24px;right:24px;z-index:9999;display:flex;flex-direction:column;gap:10px;pointer-events:none;';
            document.body.appendChild(container);
        }
        const toast = document.createElement('div');
        toast.className = `toast-message toast-${type}`;
        toast.style.cssText = `
            pointer-events:auto;
            background: ${type === 'success' ? '#10b981' : type === 'error' ? '#ef4444' : '#3b82f6'};
            color: #ffffff;
            padding: 12px 18px;
            border-radius: 8px;
            font-size: 0.88rem;
            font-weight: 600;
            box-shadow: 0 10px 25px rgba(0,0,0,0.25);
            display: flex;
            align-items: center;
            gap: 8px;
            transition: all 0.3s cubic-bezier(0.16, 1, 0.3, 1);
            transform: translateY(20px);
            opacity: 0;
        `;
        toast.innerHTML = message;
        container.appendChild(toast);
        requestAnimationFrame(() => {
            toast.style.transform = 'translateY(0)';
            toast.style.opacity = '1';
        });
        setTimeout(() => {
            toast.style.transform = 'translateY(-10px)';
            toast.style.opacity = '0';
            setTimeout(() => toast.remove(), 300);
        }, 4000);
    },

    renderAuthorAvatar(avatar, name = 'Learner', className = 'author-avatar-chip') {
        if (!avatar) {
            return `<span class="${className}">👤</span>`;
        }
        if (typeof avatar === 'string' && (avatar.startsWith('http://') || avatar.startsWith('https://') || avatar.startsWith('data:') || avatar.startsWith('/'))) {
            return `<img src="${avatar}" alt="${this.escapeHtml(name)}" class="${className} author-avatar-img">`;
        }
        return `<span class="${className}">${avatar}</span>`;
    }
};

window.App = App;
