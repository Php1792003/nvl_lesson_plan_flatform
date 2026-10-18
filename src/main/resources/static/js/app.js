document.addEventListener('DOMContentLoaded', () => {
    // Auto dismiss flash alerts with extended duration (10 seconds), hover pause, and manual close
    const flashAlerts = document.querySelectorAll('.content-body > .alert, .auth-card > .alert, .alert.alert-dismissible, .flash-alert');
    flashAlerts.forEach(alert => {
        let dismissTimer = null;

        const startTimer = (duration = 10000) => {
            clearTimeout(dismissTimer);
            dismissTimer = setTimeout(() => {
                dismissAlert(alert);
            }, duration);
        };

        const dismissAlert = (el) => {
            el.style.transition = 'opacity 0.4s ease, transform 0.4s ease, max-height 0.4s ease, margin 0.4s ease';
            el.style.opacity = '0';
            el.style.transform = 'translateY(-6px)';
            setTimeout(() => {
                if (el.parentNode) {
                    el.remove();
                }
            }, 400);
        };

        // If no close button exists yet, create one
        let closeBtn = alert.querySelector('.alert-close, [data-alert-close]');
        if (!closeBtn) {
            closeBtn = document.createElement('button');
            closeBtn.type = 'button';
            closeBtn.className = 'alert-close';
            closeBtn.setAttribute('aria-label', 'Đóng thông báo');
            closeBtn.innerHTML = '&times;';
            alert.appendChild(closeBtn);
        }

        closeBtn.addEventListener('click', (e) => {
            e.preventDefault();
            clearTimeout(dismissTimer);
            dismissAlert(alert);
        });

        // Start initial 10-second timer (gives user plenty of time to read)
        startTimer(10000);

        // Pause timer on hover so user can read without rush, resume with 5s countdown on mouse leave
        alert.addEventListener('mouseenter', () => {
            clearTimeout(dismissTimer);
        });
        alert.addEventListener('mouseleave', () => {
            startTimer(5000);
        });
    });

    // Modal open/close handlers
    document.querySelectorAll('[data-modal-target]').forEach(trigger => {
        trigger.addEventListener('click', (e) => {
            e.preventDefault();
            const targetId = trigger.getAttribute('data-modal-target');
            const modal = document.getElementById(targetId);
            if (modal) {
                modal.classList.add('open', 'active');
            }
        });
    });

    document.querySelectorAll('[data-modal-close]').forEach(closeBtn => {
        closeBtn.addEventListener('click', () => {
            const modal = closeBtn.closest('.modal-overlay');
            if (modal) {
                modal.classList.remove('open', 'active');
            }
        });
    });

    // Close modal on clicking outside
    document.querySelectorAll('.modal-overlay').forEach(overlay => {
        overlay.addEventListener('click', (e) => {
            if (e.target === overlay) {
                overlay.classList.remove('open', 'active');
            }
        });
    });

    // ==========================================
    // File Drag & Drop support (Ngăn browser tự tải file về)
    // ==========================================

    // Ngăn chặn hành vi mặc định của toàn màn hình khi kéo file vào browser
    window.addEventListener('dragover', (e) => {
        e.preventDefault();
    }, false);
    window.addEventListener('drop', (e) => {
        e.preventDefault();
    }, false);

    // Xử lý các vùng kéo thả (.file-dropzone)
    document.querySelectorAll('.file-dropzone').forEach(dropzone => {
        const fileInput = dropzone.parentElement.querySelector('input[type="file"]') || document.getElementById('file');
        const chosenText = dropzone.querySelector('#file-chosen') || dropzone.parentElement.querySelector('#file-chosen');

        // Nhấp vào vùng dropzone để mở hộp thoại chọn tệp
        dropzone.addEventListener('click', (e) => {
            if (fileInput && e.target !== fileInput) {
                fileInput.click();
            }
        });

        ['dragenter', 'dragover'].forEach(eventName => {
            dropzone.addEventListener(eventName, (e) => {
                e.preventDefault();
                e.stopPropagation();
                dropzone.classList.add('dragover');
            }, false);
        });

        ['dragleave', 'dragend'].forEach(eventName => {
            dropzone.addEventListener(eventName, (e) => {
                e.preventDefault();
                e.stopPropagation();
                dropzone.classList.remove('dragover');
            }, false);
        });

        dropzone.addEventListener('drop', (e) => {
            e.preventDefault();
            e.stopPropagation();
            dropzone.classList.remove('dragover');

            if (e.dataTransfer && e.dataTransfer.files && e.dataTransfer.files.length > 0) {
                if (fileInput) {
                    fileInput.files = e.dataTransfer.files;
                    fileInput.dispatchEvent(new Event('change', { bubbles: true }));
                }
            }
        }, false);

        if (fileInput && chosenText) {
            fileInput.addEventListener('change', () => {
                if (fileInput.files && fileInput.files[0]) {
                    const f = fileInput.files[0];
                    const sizeFormatted = f.size > 1024 * 1024
                        ? (f.size / (1024 * 1024)).toFixed(2) + ' MB'
                        : (f.size / 1024).toFixed(1) + ' KB';
                    chosenText.innerHTML = `<span style="display: inline-flex; align-items: center; gap: 6px; color: #16a34a; background: #f0fdf4; padding: 6px 12px; border-radius: 6px; border: 1px solid #bbf7d0;">
                        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M20 6L9 17l-5-5"></path></svg>
                        <span>Đã chọn: <strong>${escapeHtml(f.name)}</strong> (${sizeFormatted})</span>
                    </span>`;
                } else {
                    chosenText.innerHTML = '';
                }
            });
        }
    });

    function escapeHtml(str) {
        const div = document.createElement('div');
        div.textContent = str;
        return div.innerHTML;
    }

    // ==========================================
    // Responsive Mobile / Tablet Sidebar Drawer
    // ==========================================
    const sidebarToggleBtn = document.getElementById('sidebarToggleBtn');
    const sidebarCloseBtn = document.getElementById('sidebarCloseBtn');
    const sidebarBackdrop = document.getElementById('sidebarBackdrop');

    function openSidebar() {
        document.body.classList.add('sidebar-open');
    }

    function closeSidebar() {
        document.body.classList.remove('sidebar-open');
    }

    if (sidebarToggleBtn) {
        sidebarToggleBtn.addEventListener('click', (e) => {
            e.stopPropagation();
            if (document.body.classList.contains('sidebar-open')) {
                closeSidebar();
            } else {
                openSidebar();
            }
        });
    }

    if (sidebarCloseBtn) {
        sidebarCloseBtn.addEventListener('click', (e) => {
            e.stopPropagation();
            closeSidebar();
        });
    }

    if (sidebarBackdrop) {
        sidebarBackdrop.addEventListener('click', () => {
            closeSidebar();
        });
    }

    // Close on Escape key
    document.addEventListener('keydown', (e) => {
        if (e.key === 'Escape' && document.body.classList.contains('sidebar-open')) {
            closeSidebar();
        }
    });

    // Close sidebar on link click on tablet & smartphone (< 1024px)
    document.querySelectorAll('.sidebar-link').forEach(link => {
        link.addEventListener('click', () => {
            if (window.innerWidth < 1024) {
                closeSidebar();
            }
        });
    });

    // Reset drawer state when window is resized to desktop (>= 1024px)
    window.addEventListener('resize', () => {
        if (window.innerWidth >= 1024 && document.body.classList.contains('sidebar-open')) {
            closeSidebar();
        }
    });

    // ==========================================
    // Unified Centered Confirmation Modal Dialog
    // ==========================================
    let currentConfirmCallback = null;
    let currentCancelCallback = null;

    function getOrCreateConfirmModal() {
        let modal = document.getElementById('globalConfirmModal');
        if (!modal) {
            modal = document.createElement('div');
            modal.id = 'globalConfirmModal';
            modal.className = 'confirm-modal-overlay';
            modal.setAttribute('role', 'dialog');
            modal.setAttribute('aria-modal', 'true');
            modal.setAttribute('aria-hidden', 'true');
            modal.setAttribute('aria-labelledby', 'confirmModalTitle');
            modal.innerHTML = `
                <div class="confirm-modal-box">
                    <button type="button" class="confirm-modal-close" id="confirmModalCloseX" aria-label="Đóng">&times;</button>
                    <div class="confirm-modal-icon-wrap danger" id="confirmModalIconWrap">
                        <svg id="confirmModalIconSvg" width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round">
                            <path d="M10.29 3.86L1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0z"></path>
                            <line x1="12" y1="9" x2="12" y2="13"></line>
                            <line x1="12" y1="17" x2="12.01" y2="17"></line>
                        </svg>
                    </div>
                    <h3 class="confirm-modal-title" id="confirmModalTitle">Xác nhận thao tác</h3>
                    <p class="confirm-modal-message" id="confirmModalMessage">Bạn có chắc chắn muốn thực hiện thao tác này không?</p>
                    <div class="confirm-modal-actions">
                        <button type="button" class="confirm-modal-btn cancel" id="confirmModalCancelBtn">Huỷ bỏ</button>
                        <button type="button" class="confirm-modal-btn submit danger" id="confirmModalSubmitBtn">Xác nhận</button>
                    </div>
                </div>
            `;
            document.body.appendChild(modal);
            bindConfirmModalEvents(modal);
        }
        return modal;
    }

    function bindConfirmModalEvents(modal) {
        if (!modal || modal.dataset.bound === 'true') return;
        modal.dataset.bound = 'true';

        const cancelBtn = modal.querySelector('#confirmModalCancelBtn');
        const closeX = modal.querySelector('#confirmModalCloseX');
        const submitBtn = modal.querySelector('#confirmModalSubmitBtn');

        const doCancel = () => {
            const onCancel = currentCancelCallback;
            closeConfirmModal();
            if (typeof onCancel === 'function') {
                onCancel();
            }
        };

        if (cancelBtn) cancelBtn.addEventListener('click', doCancel);
        if (closeX) closeX.addEventListener('click', doCancel);

        modal.addEventListener('click', (e) => {
            if (e.target === modal) {
                doCancel();
            }
        });

        if (submitBtn) {
            submitBtn.addEventListener('click', () => {
                const onConfirm = currentConfirmCallback;
                closeConfirmModal();
                if (typeof onConfirm === 'function') {
                    onConfirm();
                }
            });
        }
    }

    function closeConfirmModal() {
        const modal = document.getElementById('globalConfirmModal');
        if (modal) {
            modal.classList.remove('open');
            modal.setAttribute('aria-hidden', 'true');
        }
        currentConfirmCallback = null;
        currentCancelCallback = null;
    }

    window.closeConfirmModal = closeConfirmModal;

    window.showConfirmModal = function (opts = {}) {
        return new Promise((resolve) => {
            const modal = getOrCreateConfirmModal();
            bindConfirmModalEvents(modal);

            const titleEl = modal.querySelector('#confirmModalTitle');
            const msgEl = modal.querySelector('#confirmModalMessage');
            const submitBtn = modal.querySelector('#confirmModalSubmitBtn');
            const cancelBtn = modal.querySelector('#confirmModalCancelBtn');
            const iconWrap = modal.querySelector('#confirmModalIconWrap');
            const iconSvg = modal.querySelector('#confirmModalIconSvg');

            const type = opts.type || 'danger';
            const title = opts.title || (type === 'danger' ? 'Xác nhận xoá' : 'Xác nhận thao tác');
            const message = opts.message || 'Bạn có chắc chắn muốn thực hiện thao tác này?';
            const confirmText = opts.confirmText || (type === 'danger' ? 'Xác nhận xoá' : 'Xác nhận');
            const cancelText = opts.cancelText || 'Huỷ bỏ';

            if (titleEl) titleEl.textContent = title;
            if (msgEl) msgEl.textContent = message;

            if (submitBtn) {
                submitBtn.textContent = confirmText;
                submitBtn.className = `confirm-modal-btn submit ${type}`;
            }
            if (cancelBtn) {
                cancelBtn.textContent = cancelText;
            }

            if (iconWrap) {
                iconWrap.className = `confirm-modal-icon-wrap ${type}`;
            }

            if (iconSvg) {
                if (type === 'danger') {
                    iconSvg.innerHTML = `
                        <path d="M10.29 3.86L1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0z"></path>
                        <line x1="12" y1="9" x2="12" y2="13"></line>
                        <line x1="12" y1="17" x2="12.01" y2="17"></line>
                    `;
                } else if (type === 'warning') {
                    iconSvg.innerHTML = `
                        <circle cx="12" cy="12" r="10"></circle>
                        <line x1="12" y1="8" x2="12" y2="12"></line>
                        <line x1="12" y1="16" x2="12.01" y2="16"></line>
                    `;
                } else {
                    iconSvg.innerHTML = `
                        <circle cx="12" cy="12" r="10"></circle>
                        <path d="M9.09 9a3 3 0 0 1 5.83 1c0 2-3 3-3 3"></path>
                        <line x1="12" y1="17" x2="12.01" y2="17"></line>
                    `;
                }
            }

            currentConfirmCallback = () => {
                if (typeof opts.onConfirm === 'function') opts.onConfirm();
                resolve(true);
            };
            currentCancelCallback = () => {
                if (typeof opts.onCancel === 'function') opts.onCancel();
                resolve(false);
            };

            modal.classList.add('open');
            modal.setAttribute('aria-hidden', 'false');

            // Focus cancel button for safe default
            if (cancelBtn) {
                setTimeout(() => cancelBtn.focus(), 60);
            }
        });
    };

    // Close on Escape key
    document.addEventListener('keydown', (e) => {
        if (e.key === 'Escape') {
            const modal = document.getElementById('globalConfirmModal');
            if (modal && modal.classList.contains('open')) {
                closeConfirmModal();
            }
        }
    });

    // Auto bind if already in DOM from template
    const existingModal = document.getElementById('globalConfirmModal');
    if (existingModal) {
        bindConfirmModalEvents(existingModal);
    }

    // Convert legacy inline onsubmit="return confirm('...')" into modern data-confirm
    function upgradeLegacyConfirmForms() {
        document.querySelectorAll('form[onsubmit*="confirm("]').forEach(form => {
            const onsubmitStr = form.getAttribute('onsubmit') || '';
            const match = onsubmitStr.match(/confirm\s*\(\s*(['"])([\s\S]*?)\1\s*\)/i);
            if (match && match[2]) {
                form.removeAttribute('onsubmit');
                form.setAttribute('data-confirm', match[2].trim());
                if (!form.getAttribute('data-confirm-title')) {
                    form.setAttribute('data-confirm-title', 'Xác nhận thao tác');
                }
                if (!form.getAttribute('data-confirm-type')) {
                    form.setAttribute('data-confirm-type', 'danger');
                }
            }
        });
    }
    upgradeLegacyConfirmForms();

    // Global listener for forms with data-confirm
    document.addEventListener('submit', function (e) {
        const form = e.target;
        if (!form || !form.getAttribute) return;

        const confirmMsg = form.getAttribute('data-confirm');
        if (!confirmMsg) return;

        if (form.dataset.confirmed === 'true') {
            delete form.dataset.confirmed;
            return; // Allowed through after modal confirmation
        }

        e.preventDefault();
        e.stopImmediatePropagation();

        const title = form.getAttribute('data-confirm-title') || 'Xác nhận thao tác';
        const confirmBtn = form.getAttribute('data-confirm-btn') || 'Xác nhận';
        const cancelBtn = form.getAttribute('data-confirm-cancel') || 'Huỷ bỏ';
        const type = form.getAttribute('data-confirm-type') || 'danger';

        window.showConfirmModal({
            title: title,
            message: confirmMsg,
            confirmText: confirmBtn,
            cancelText: cancelBtn,
            type: type,
            onConfirm: () => {
                form.dataset.confirmed = 'true';
                if (typeof form.requestSubmit === 'function') {
                    form.requestSubmit();
                } else {
                    form.submit();
                }
            }
        });
    }, true);

    // Global listener for all clickable elements with data-confirm (links, buttons)
    document.addEventListener('click', function (e) {
        const target = e.target.closest('[data-confirm]');
        if (!target || target.tagName === 'FORM') return;

        if (target.dataset.confirmed === 'true') {
            delete target.dataset.confirmed;
            return;
        }

        e.preventDefault();
        e.stopImmediatePropagation();

        const confirmMsg = target.getAttribute('data-confirm');
        const title = target.getAttribute('data-confirm-title') || 'Xác nhận thao tác';
        const confirmBtn = target.getAttribute('data-confirm-btn') || (target.getAttribute('data-confirm-type') === 'danger' ? 'Xác nhận' : 'Đồng ý');
        const cancelBtn = target.getAttribute('data-confirm-cancel') || 'Huỷ bỏ';
        const type = target.getAttribute('data-confirm-type') || 'warning';

        window.showConfirmModal({
            title: title,
            message: confirmMsg,
            confirmText: confirmBtn,
            cancelText: cancelBtn,
            type: type,
            onConfirm: () => {
                target.dataset.confirmed = 'true';
                if (target.tagName === 'A' && target.href) {
                    window.location.href = target.href;
                } else if (target.type === 'submit' && target.form) {
                    if (typeof target.form.requestSubmit === 'function') {
                        target.form.requestSubmit(target);
                    } else {
                        target.form.submit();
                    }
                } else {
                    target.click();
                }
            }
        });
    }, true);

    // ==========================================
    // URL-based Sidebar Active Highlight Fallback
    // ==========================================
    function highlightActiveSidebarLink() {
        const path = window.location.pathname;
        const sidebarLinks = Array.from(document.querySelectorAll('.sidebar-link'));
        if (!sidebarLinks.length) return;

        // If a link is already marked active by server, check if it's already good
        const currentActive = sidebarLinks.find(l => l.classList.contains('active'));
        if (currentActive) {
            return;
        }

        let bestMatch = null;
        let maxMatchLen = 0;

        sidebarLinks.forEach(link => {
            const href = link.getAttribute('href');
            if (!href) return;
            const cleanHref = href.split('?')[0].split('#')[0];
            if (path === cleanHref) {
                bestMatch = link;
                maxMatchLen = 9999;
            } else if (cleanHref !== '/' && path.startsWith(cleanHref) && cleanHref.length > maxMatchLen) {
                bestMatch = link;
                maxMatchLen = cleanHref.length;
            }
        });

        if (bestMatch) {
            sidebarLinks.forEach(l => l.classList.remove('active'));
            bestMatch.classList.add('active');
        }
    }
    highlightActiveSidebarLink();

    // ==========================================
    // Light / Dark Mode Theme Manager
    // ==========================================
    const themeToggleBtn = document.getElementById('themeToggleBtn');

    function getCurrentTheme() {
        return document.documentElement.getAttribute('data-theme') ||
            localStorage.getItem('nvl_theme') ||
            (window.matchMedia('(prefers-color-scheme: dark)').matches ? 'dark' : 'light');
    }

    function applyTheme(theme) {
        document.documentElement.setAttribute('data-theme', theme);
        try {
            localStorage.setItem('nvl_theme', theme);
        } catch (e) { }

        if (themeToggleBtn) {
            themeToggleBtn.setAttribute('title', theme === 'dark' ? 'Chuyển sang giao diện Sáng (Light)' : 'Chuyển sang giao diện Tối (Dark)');
            themeToggleBtn.setAttribute('aria-label', theme === 'dark' ? 'Giao diện tối đang bật' : 'Giao diện sáng đang bật');
        }

        window.dispatchEvent(new CustomEvent('nvl-theme-changed', { detail: { theme } }));
    }

    // Initialize state
    applyTheme(getCurrentTheme());

    if (themeToggleBtn) {
        themeToggleBtn.addEventListener('click', (e) => {
            e.preventDefault();
            const current = getCurrentTheme();
            const nextTheme = current === 'dark' ? 'light' : 'dark';
            applyTheme(nextTheme);
        });
    }

    // ==========================================
    // Language Switcher & i18n Manager
    // ==========================================
    const langSwitchBtn = document.getElementById('langSwitchBtn');
    const langDropdownMenu = document.getElementById('langDropdownMenu');
    const currentLangFlag = document.getElementById('currentLangFlag');
    const currentLangCode = document.getElementById('currentLangCode');

    function getCookie(name) {
        const matches = document.cookie.match(new RegExp('(?:^|; )' + name.replace(/([\.$?*|{}\(\)\[\]\\\/\+^])/g, '\\$1') + '=([^;]*)'));
        return matches ? decodeURIComponent(matches[1]) : undefined;
    }

    function setCookie(name, value, days = 365) {
        const d = new Date();
        d.setTime(d.getTime() + (days * 24 * 60 * 60 * 1000));
        document.cookie = `${name}=${encodeURIComponent(value)};expires=${d.toUTCString()};path=/;SameSite=Lax`;
    }

    function detectCurrentLang() {
        const urlParams = new URLSearchParams(window.location.search);
        const urlLang = urlParams.get('lang');
        if (urlLang === 'en' || urlLang === 'vi') return urlLang;

        const cookieLang = getCookie('nvl_lang');
        if (cookieLang === 'en' || cookieLang === 'vi') return cookieLang;

        const storedLang = localStorage.getItem('nvl_lang');
        if (storedLang === 'en' || storedLang === 'vi') return storedLang;

        return 'vi';
    }

    const currentLang = detectCurrentLang();

    // Update Language UI representation
    function updateLangUI(lang) {
        if (currentLangFlag) {
            currentLangFlag.textContent = lang === 'en' ? '🇺🇸' : '🇻🇳';
        }
        if (currentLangCode) {
            currentLangCode.textContent = lang === 'en' ? 'EN' : 'VI';
        }
        document.querySelectorAll('.lang-dropdown-item').forEach(item => {
            const itemLang = item.getAttribute('data-lang');
            if (itemLang === lang) {
                item.classList.add('active');
            } else {
                item.classList.remove('active');
            }
        });
    }

    updateLangUI(currentLang);

    if (langSwitchBtn && langDropdownMenu) {
        langSwitchBtn.addEventListener('click', (e) => {
            e.stopPropagation();
            const isExpanded = langDropdownMenu.classList.toggle('show');
            langSwitchBtn.setAttribute('aria-expanded', isExpanded);
        });

        document.querySelectorAll('.lang-dropdown-item').forEach(item => {
            item.addEventListener('click', (e) => {
                e.preventDefault();
                const selectedLang = item.getAttribute('data-lang');
                if (selectedLang) {
                    setCookie('nvl_lang', selectedLang, 365);
                    try {
                        localStorage.setItem('nvl_lang', selectedLang);
                    } catch (e) { }

                    // Build target URL preserving other parameters and hash
                    const url = new URL(window.location.href);
                    url.searchParams.set('lang', selectedLang);
                    window.location.href = url.toString();
                }
            });
        });

        // Close language dropdown on outside click
        document.addEventListener('click', (e) => {
            if (!e.target.closest('#langSwitchWrapper')) {
                langDropdownMenu.classList.remove('show');
                langSwitchBtn.setAttribute('aria-expanded', 'false');
            }
        });
    }

    // ==========================================
    // Client-side Fallback Translator (for Dynamic & Database content in EN)
    // ==========================================
    if (currentLang === 'en') {
        const viToEnDict = {
            'Toán học': 'Mathematics',
            'Âm nhạc': 'Music',
            'Tin học': 'Informatics',
            'Vật lý': 'Physics',
            'Hóa học': 'Chemistry',
            'Sinh học': 'Biology',
            'Ngữ văn': 'Literature',
            'Lịch sử': 'History',
            'Địa lý': 'Geography',
            'Tiếng Anh': 'English',
            'GDCD': 'Civic Education',
            'Thể dục': 'Physical Education',
            'Công nghệ': 'Technology',
            'Mỹ thuật': 'Fine Arts',
            'Điểm trường chính': 'Main Campus',
            'Phân hiệu 1': 'Campus 1',
            'Phân hiệu 2': 'Campus 2',
            'Ban Giám Hiệu': 'School Board',
            'Giáo viên': 'Teacher',
            'Tất cả bộ môn': 'All subjects',
            'Tất cả năm học': 'All school years',
            'Tất cả các khối': 'All grades',
            'Tất cả': 'All',
            'Bình thường': 'Regular',
            'Bảo vệ học kỳ': 'Semester Protection',
            'Được bảo vệ': 'Protected',
            'giáo án phù hợp': 'matching plans',
            'giáo án': 'plans',
            'Xem trước': 'Preview',
            'Nhận xét': 'Review',
            'Xem': 'View',
            'Tải lên': 'Upload',
            'Lọc': 'Filter',
            'Bỏ lọc': 'Clear filter',
            'Đang lọc:': 'Filtering by:',
            'Tiêu đề giáo án': 'Lesson Plan Title',
            'Bộ môn': 'Subject',
            'Khối': 'Grade',
            'Năm học': 'School Year',
            'Phiên bản': 'Version',
            'Ngày tải': 'Upload Date',
            'Đánh giá': 'Evaluation',
            'Thao tác': 'Actions',
            'Thời hạn': 'Expiry',
            'Vô thời hạn': 'Unlimited',
            'Người chia sẻ': 'Shared By',
            'Người soạn': 'Author',
            'Giáo án của tôi': 'My Lesson Plans',
            'Được chia sẻ với tôi': 'Shared with Me',
            'Yêu thích': 'Favorite',
            'Yêu thích ': 'Favorite (Protected)',
            '⭐ Yêu thích ': '⭐ Favorite (Protected)',
            '⭐ Yêu thích': '⭐ Favorite',
            '☆ Thích': '☆ Like',
            '⭐ Đã thích': '⭐ Liked',
            '⭐ Bỏ thích': '⭐ Unlike',
            'Hiển thị': 'Showing',
            'trên tổng': 'of',
            'Trước': 'Previous',
            'Sau': 'Next',
            'Trang': 'Page',
            'trên trang này': 'on this page',
            'Không tìm thấy giáo án nào': 'No lesson plans found',
            'Không tìm thấy giáo án phù hợp': 'No matching lesson plans found',
            'Dọn dẹp giáo án sau học kỳ': 'Semester Lesson Plan Cleanup',
            'Dọn dẹp giáo án học kỳ': 'Semester Lesson Plan Cleanup',
            'Tiến hành dọn dẹp': 'Execute Cleanup',
            'Huỷ': 'Cancel',
            'Hủy': 'Cancel'
        };

        function translateElementTree(root) {
            if (!root) return;
            const walker = document.createTreeWalker(root, NodeFilter.SHOW_TEXT, null, false);
            let node;
            while ((node = walker.nextNode())) {
                const parent = node.parentElement;
                if (!parent || parent.tagName === 'SCRIPT' || parent.tagName === 'STYLE') continue;
                let text = node.nodeValue.trim();
                if (text && viToEnDict[text]) {
                    node.nodeValue = node.nodeValue.replace(text, viToEnDict[text]);
                } else if (text) {
                    let updated = node.nodeValue;
                    updated = updated.replace(/\bTuần\s+(\d+)/g, 'Week $1');
                    updated = updated.replace(/\bKhối\s+(\d+)/g, 'Grade $1');
                    updated = updated.replace(/\bHọc kỳ\s+(\d+)/g, 'Semester $1');
                    updated = updated.replace(/\bTiết\s+(\d+)\s*-\s*(\d+)\s*\((\d+)\s*tiết\)/g, 'Period $1 - $2 ($3 periods)');
                    updated = updated.replace(/\b(\d+)\s*tiết\b/g, '$1 periods');
                    if (updated !== node.nodeValue) {
                        node.nodeValue = updated;
                    }
                }
            }

            root.querySelectorAll('option').forEach(opt => {
                const val = opt.textContent.trim();
                if (viToEnDict[val]) {
                    opt.textContent = viToEnDict[val];
                }
            });

            root.querySelectorAll('input[placeholder]').forEach(inp => {
                const ph = inp.getAttribute('placeholder');
                if (ph && viToEnDict[ph]) {
                    inp.setAttribute('placeholder', viToEnDict[ph]);
                }
            });
        }

        translateElementTree(document.body);
    }
});


