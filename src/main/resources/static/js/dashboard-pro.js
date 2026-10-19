/**
 * THCS Nguyễn Văn Linh — Lesson Plan Platform (Dashboard Pro Shared JavaScript)
 * Provides smooth transitions, shared utilities, and page initializers.
 */
(function (global) {
    'use strict';

    var NVL = global.NVL = global.NVL || {};

    NVL.reduceMotion = window.matchMedia('(prefers-reduced-motion: reduce)').matches;

    NVL.$ = function (id) {
        return document.getElementById(id);
    };

    NVL.setText = function (id, txt) {
        var el = NVL.$(id);
        if (el) el.textContent = txt;
    };

    /**
     * Chuẩn hoá chuỗi tiếng Việt (bỏ dấu, chữ thường) để tìm kiếm không dấu
     */
    NVL.norm = function (s) {
        return String(s || '').toLowerCase()
            .normalize('NFD').replace(/[\u0300-\u036f]/g, '')
            .replace(/đ/g, 'd');
    };

    /**
     * Hiệu ứng chuyển trang mượt mà qua thanh tiến trình #navProgress
     */
    NVL.setupPageTransitions = function () {
        if (window.__pageTransitionBound) return;
        window.__pageTransitionBound = true;
        var bar = NVL.$('navProgress');

        document.addEventListener('click', function (e) {
            if (e.defaultPrevented || e.button !== 0 || e.metaKey || e.ctrlKey || e.shiftKey || e.altKey) return;
            var a = e.target.closest ? e.target.closest('a[href]') : null;
            if (!a) return;
            var href = a.getAttribute('href');
            if (!href || href.charAt(0) === '#' || href.indexOf('javascript:') === 0) return;
            if ((a.target && a.target !== '_self') || a.hasAttribute('download') || a.hasAttribute('data-no-transition')) return;

            var url;
            try { url = new URL(a.href, window.location.href); } catch (err) { return; }
            if (url.origin !== window.location.origin) return;
            if (url.pathname === window.location.pathname && url.search === window.location.search) return;

            e.preventDefault();
            var r = document.querySelector('.dash, .upl, .plans, .adm, .usr, .rol');
            if (r) r.classList.add('is-leaving');
            if (bar) bar.classList.add('run');
            setTimeout(function () { window.location.href = url.href; }, NVL.reduceMotion ? 0 : 200);
        });

        window.addEventListener('pageshow', function (e) {
            if (e.persisted) {
                var r = document.querySelector('.dash, .upl, .plans, .adm, .usr, .rol');
                if (r) r.classList.remove('is-leaving', 'is-loading');
                if (bar) bar.classList.remove('run');
            }
        });
    };

    /**
     * Gán thứ tự hoạt ảnh xuất hiện lần lượt cho các hàng trong bảng
     */
    NVL.setupRowDelays = function (selector) {
        var rows = Array.prototype.slice.call(document.querySelectorAll(selector));
        rows.forEach(function (tr, i) { tr.style.setProperty('--r', Math.min(i, 10)); });
        return rows;
    };

    /**
     * Bấm vào bất kỳ đâu trên dòng bảng để mở liên kết chi tiết
     */
    NVL.setupRowClicks = function (rows, linkSelector) {
        rows.forEach(function (tr) {
            tr.addEventListener('click', function (e) {
                if (e.target.closest('a, button, input, select, form')) return;
                var link = tr.querySelector(linkSelector || 'a[href]');
                if (link) link.click();
            });
        });
    };

    /**
     * Tạo avatar chữ cái đầu và màu sắc cố định dựa trên tên người dùng
     */
    NVL.setupAvatars = function (containerSelector, avatarSelector, nameSelector) {
        var colors = ['#1d4ed8', '#0d9488', '#7c3aed', '#b45309', '#be123c', '#0e7490', '#4d7c0f', '#475569'];
        document.querySelectorAll(containerSelector).forEach(function (o) {
            var nameEl = o.querySelector(nameSelector);
            var av = o.querySelector(avatarSelector);
            if (!nameEl || !av) return;
            var nm = (nameEl.textContent || '').trim();
            var words = nm.split(/\s+/).filter(Boolean);
            var last = words.length ? words[words.length - 1] : '?';
            av.textContent = last.charAt(0).toUpperCase();
            if (!o.classList.contains('gone')) {
                var h = 0;
                for (var i = 0; i < nm.length; i++) h = (h * 31 + nm.charCodeAt(i)) | 0;
                av.style.background = colors[Math.abs(h) % colors.length];
            }
        });
    };

    /* ==========================================================================
       MODULE 1: DASHBOARD OVERVIEW (.dash)
       ========================================================================== */
    NVL.initDashboard = function (config) {
        var root = NVL.$('dashRoot');
        if (!root) return;

        var nf = new Intl.NumberFormat('vi-VN');
        var series = (config.monthlyData || []).map(function (v) { return Number(v) || 0; });
        var labels = config.monthlyLabels || [];
        var deptLabels = config.deptLabels || [];
        var deptData = config.deptData || [];
        var isAdmin = !!config.isAdmin;
        var palette = ['#1d4ed8', '#0d9488', '#d97706', '#7c3aed', '#e11d48', '#0891b2', '#65a30d', '#64748b'];

        /* Ngày hiện tại */
        var dateEl = NVL.$('dashDate');
        if (dateEl) {
            var d = new Date().toLocaleDateString('vi-VN', { weekday: 'long', day: '2-digit', month: '2-digit', year: 'numeric' });
            dateEl.textContent = d.charAt(0).toUpperCase() + d.slice(1);
        }

        /* Chỉ số thống kê theo tháng */
        var n = series.length;
        var thisMonth = n ? series[n - 1] : 0;
        var prevMonth = n > 1 ? series[n - 2] : null;
        var sum = series.reduce(function (s, v) { return s + v; }, 0);
        var avg = n ? sum / n : 0;
        var peakIdx = 0;
        series.forEach(function (v, i) { if (v > series[peakIdx]) peakIdx = i; });

        var kThis = NVL.$('kpiThisMonth'); if (kThis) kThis.setAttribute('data-value', thisMonth);
        var kDepts = NVL.$('kpiDepts'); if (kDepts) kDepts.setAttribute('data-value', deptLabels.length);
        var kMonths = NVL.$('kpiMonths'); if (kMonths && n) kMonths.setAttribute('data-value', n);

        NVL.setText('stAvg', n ? avg.toLocaleString('vi-VN', { maximumFractionDigits: 1 }) + ' giáo án' : '—');
        NVL.setText('stPeak', n && series[peakIdx] > 0 ? (labels[peakIdx] || '') + ' · ' + nf.format(series[peakIdx]) : '—');
        NVL.setText('stSum', n ? nf.format(sum) + ' giáo án' : '—');

        /* Chênh lệch so với tháng trước */
        var deltaEl = NVL.$('heroDelta');
        if (deltaEl) {
            if (prevMonth === null) {
                deltaEl.textContent = 'Chưa đủ dữ liệu';
                NVL.setText('heroNote', '');
            } else if (prevMonth === 0 && thisMonth === 0) {
                deltaEl.textContent = 'Không đổi';
            } else if (prevMonth === 0) {
                deltaEl.textContent = '+' + nf.format(thisMonth) + ' mới';
                deltaEl.classList.add('up');
            } else {
                var pct = Math.round((thisMonth - prevMonth) / prevMonth * 100);
                deltaEl.textContent = (pct > 0 ? '▲ +' : pct < 0 ? '▼ ' : '') + pct + '%';
                if (pct > 0) deltaEl.classList.add('up');
                if (pct < 0) deltaEl.classList.add('down');
            }
        }
        if (n > 1) NVL.setText('sparkTitle', n + ' tháng gần nhất');

        /* Chart instances */
        var mainChart = null;
        var deptChart = null;

        /* Vào trang + hiệu ứng đếm số */
        requestAnimationFrame(function () {
            requestAnimationFrame(function () {
                root.classList.add('is-ready');
                document.querySelectorAll('.dept-bar b').forEach(function (b) { b.style.width = b.getAttribute('data-w') + '%'; });
                setTimeout(function () {
                    if (mainChart) mainChart.resize();
                    if (deptChart) deptChart.resize();
                }, 100);
            });
        });

        /* Tự động cân chỉnh biểu đồ khi xoay màn hình hoặc đổi kích thước cửa sổ */
        var rzTimer;
        window.addEventListener('resize', function () {
            clearTimeout(rzTimer);
            rzTimer = setTimeout(function () {
                if (mainChart) mainChart.resize();
                if (deptChart) deptChart.resize();
            }, 80);
        });

        function countUp(el) {
            var raw = String(el.getAttribute('data-value') || el.textContent).replace(',', '.');
            var target = parseFloat(raw);
            if (isNaN(target)) return;
            var dot = raw.indexOf('.');
            var decimals = dot >= 0 ? Math.min(raw.length - dot - 1, 2) : 0;
            var fmt = new Intl.NumberFormat('vi-VN', { minimumFractionDigits: decimals, maximumFractionDigits: decimals });
            if (NVL.reduceMotion || target === 0) { el.textContent = fmt.format(target); return; }
            var start = performance.now(), duration = 1000;
            function tick(now) {
                var p = Math.min((now - start) / duration, 1);
                el.textContent = fmt.format(target * (1 - Math.pow(1 - p, 4)));
                if (p < 1) requestAnimationFrame(tick);
            }
            requestAnimationFrame(tick);
        }
        document.querySelectorAll('[data-count]').forEach(countUp);

        /* Chart.js Configuration */
        if (typeof Chart === 'undefined') return;

        var fontFamily = getComputedStyle(root).fontFamily;
        Chart.defaults.font.family = fontFamily;
        Chart.defaults.color = '#6b7a96';
        Chart.defaults.font.size = 12;

        var anim = NVL.reduceMotion ? false : { duration: 900, easing: 'easeOutQuart' };
        var tip = {
            backgroundColor: '#0b1b3a', titleColor: '#fff', bodyColor: '#d7e0f2',
            padding: 12, cornerRadius: 10, displayColors: false,
            callbacks: { label: function (c) { return nf.format(c.parsed.y) + ' giáo án'; } }
        };

        function ready(canvas, values) {
            var box = canvas.parentElement;
            box.classList.remove('loading');
            var has = values.some(function (v) { return Number(v) > 0; });
            if (!has) { var em = box.querySelector('.chart-empty'); if (em) em.classList.add('show'); }
            return has;
        }

        function grad(chart, c1, c2) {
            var a = chart.chartArea;
            if (!a) return c1;
            var g = chart.ctx.createLinearGradient(0, a.top, 0, a.bottom);
            g.addColorStop(0, c1); g.addColorStop(1, c2);
            return g;
        }

        /* Hero Sparkline */
        var sparkCv = NVL.$('heroSpark');
        if (sparkCv && n) {
            new Chart(sparkCv, {
                type: 'bar',
                data: {
                    labels: labels,
                    datasets: [{
                        data: series,
                        backgroundColor: series.map(function (v, i) { return i === n - 1 ? '#2dd4bf' : 'rgba(159, 179, 217, .32)'; }),
                        borderRadius: 4, borderSkipped: false, maxBarThickness: 22
                    }]
                },
                options: {
                    responsive: true, maintainAspectRatio: false, animation: anim,
                    plugins: { legend: { display: false }, tooltip: tip },
                    scales: { x: { display: false }, y: { display: false, beginAtZero: true } }
                }
            });
        }

        /* Biểu đồ chính (Cột / Đường) */
        var mainCv = NVL.$('mainChart');
        if (mainCv) ready(mainCv, series);

        function buildMain(type) {
            if (!mainCv) return;
            if (mainChart) mainChart.destroy();
            var isLine = type === 'line';
            mainChart = new Chart(mainCv, {
                type: type,
                data: {
                    labels: labels,
                    datasets: [{
                        label: isAdmin ? 'Giáo án toàn trường' : 'Số giáo án nộp',
                        data: series,
                        fill: isLine,
                        backgroundColor: function (c) {
                            return isLine ? grad(c.chart, 'rgba(29, 78, 216, .22)', 'rgba(29, 78, 216, 0)')
                                : grad(c.chart, 'rgba(29, 78, 216, .95)', 'rgba(29, 78, 216, .55)');
                        },
                        borderColor: '#1d4ed8',
                        borderWidth: isLine ? 2.5 : 0,
                        tension: .38,
                        pointRadius: 0, pointHoverRadius: 6,
                        pointBackgroundColor: '#fff', pointBorderColor: '#1d4ed8', pointBorderWidth: 2,
                        hoverBackgroundColor: '#1e40af',
                        borderRadius: isLine ? 0 : { topLeft: 7, topRight: 7 },
                        borderSkipped: false, maxBarThickness: 32
                    }]
                },
                options: {
                    responsive: true, maintainAspectRatio: false, animation: anim,
                    resizeDelay: 60,
                    interaction: { mode: 'index', intersect: false },
                    plugins: { legend: { display: false }, tooltip: tip },
                    scales: {
                        x: {
                            grid: { display: false },
                            border: { display: false },
                            ticks: {
                                autoSkip: true,
                                maxTicksLimit: 6,
                                maxRotation: 0,
                                font: { size: 11 }
                            }
                        },
                        y: {
                            beginAtZero: true, ticks: { precision: 0, padding: 8, font: { size: 11 } },
                            grid: { color: 'rgba(107, 122, 150, .16)' }, border: { display: false }
                        }
                    }
                }
            });
        }
        buildMain('bar');

        document.querySelectorAll('.seg button').forEach(function (btn) {
            btn.addEventListener('click', function () {
                document.querySelectorAll('.seg button').forEach(function (b) { b.classList.remove('on'); });
                btn.classList.add('on');
                buildMain(btn.getAttribute('data-type'));
            });
        });

        /* Môn học: Doughnut Chart + Ranking List */
        var deptCv = NVL.$('deptChart');
        if (isAdmin && deptCv) {
            var dvals = (deptData || []).map(function (v) { return Number(v) || 0; });
            var hasDept = ready(deptCv, dvals);
            var total = dvals.reduce(function (s, v) { return s + v; }, 0);

            var centerText = {
                id: 'centerText',
                afterDraw: function (chart) {
                    if (!hasDept) return;
                    var a = chart.chartArea, c = chart.ctx;
                    if (!a) return;
                    var cx = (a.left + a.right) / 2, cy = (a.top + a.bottom) / 2;
                    c.save();
                    c.textAlign = 'center'; c.textBaseline = 'middle';
                    c.fillStyle = '#0b1b3a'; c.font = '700 24px ' + fontFamily;
                    c.fillText(nf.format(total), cx, cy - 7);
                    c.fillStyle = '#6b7a96'; c.font = '500 11px ' + fontFamily;
                    c.fillText('giáo án', cx, cy + 14);
                    c.restore();
                }
            };

            if (deptChart) deptChart.destroy();
            deptChart = new Chart(deptCv, {
                type: 'doughnut',
                data: {
                    labels: deptLabels,
                    datasets: [{
                        data: dvals,
                        backgroundColor: dvals.map(function (v, i) { return palette[i % palette.length]; }),
                        borderColor: '#ffffff', borderWidth: 3, hoverOffset: 6, spacing: 1
                    }]
                },
                options: {
                    responsive: true, maintainAspectRatio: false, cutout: '70%',
                    resizeDelay: 60,
                    animation: NVL.reduceMotion ? false : { duration: 900, easing: 'easeOutQuart', animateRotate: true, animateScale: true },
                    plugins: {
                        legend: { display: false },
                        tooltip: Object.assign({}, tip, {
                            callbacks: {
                                label: function (c) {
                                    var p = total ? Math.round(c.parsed / total * 100) : 0;
                                    return c.label + ': ' + nf.format(c.parsed) + ' (' + p + '%)';
                                }
                            }
                        })
                    }
                },
                plugins: [centerText]
            });

            var list = NVL.$('deptList');
            if (list && hasDept) {
                var order = dvals.map(function (v, i) { return i; }).sort(function (a, b) { return dvals[b] - dvals[a]; });
                order.forEach(function (i) {
                    var p = total ? Math.round(dvals[i] / total * 100) : 0;
                    var li = document.createElement('li');
                    li.innerHTML =
                        '<div class="dept-name"><i></i><span></span></div>' +
                        '<div class="dept-num">' + nf.format(dvals[i]) + '<em>' + p + '%</em></div>' +
                        '<div class="dept-bar"><b data-w="' + p + '"></b></div>';
                    li.querySelector('.dept-name span').textContent = deptLabels[i];
                    li.querySelector('.dept-name i').style.background = palette[i % palette.length];
                    li.querySelector('.dept-bar b').style.background = palette[i % palette.length];
                    list.appendChild(li);
                });
                if (root.classList.contains('is-ready')) {
                    list.querySelectorAll('.dept-bar b').forEach(function (b) { b.style.width = b.getAttribute('data-w') + '%'; });
                }
            }
        }
    };

    /* ==========================================================================
       MODULE 2: PLAN UPLOAD (.upl)
       ========================================================================== */
    NVL.initPlanUpload = function () {
        var root = NVL.$('uplRoot');
        if (!root) return;

        requestAnimationFrame(function () {
            requestAnimationFrame(function () { root.classList.add('is-ready'); });
        });

        var MAX = 10 * 1024 * 1024;
        var ALLOWED = ['pdf', 'doc', 'docx', 'ppt', 'pptx'];
        var input = NVL.$('file');
        var wrap = NVL.$('dropWrap');
        var drop = NVL.$('drop');
        var err = NVL.$('fileErr');

        function fmtSize(b) {
            if (b < 1024) return b + ' B';
            if (b < 1024 * 1024) return (b / 1024).toFixed(0) + ' KB';
            return (b / 1024 / 1024).toFixed(2).replace('.', ',') + ' MB';
        }
        function showError(msg) {
            if (err) { err.textContent = msg; err.classList.add('show'); }
        }
        function clearError() {
            if (err) { err.classList.remove('show'); err.textContent = ''; }
        }

        function clearFile() {
            if (input) input.value = '';
            if (wrap) wrap.classList.remove('has-file');
            clearError();
            updateProgress();
        }

        function handleFile() {
            clearError();
            var f = input && input.files && input.files[0];
            if (!f) { if (wrap) wrap.classList.remove('has-file'); updateProgress(); return; }
            var ext = (f.name.split('.').pop() || '').toLowerCase();
            if (ALLOWED.indexOf(ext) === -1) {
                input.value = '';
                if (wrap) wrap.classList.remove('has-file');
                showError('Định dạng .' + ext + ' không được hỗ trợ. Vui lòng chọn tệp PDF, Word hoặc PowerPoint.');
                updateProgress();
                return;
            }
            if (f.size > MAX) {
                input.value = '';
                if (wrap) wrap.classList.remove('has-file');
                showError('Tệp "' + f.name + '" nặng ' + fmtSize(f.size) + ', vượt giới hạn 10 MB. Hãy nén hình ảnh trong tệp rồi thử lại.');
                updateProgress();
                return;
            }

            var badge = NVL.$('chosenExt');
            if (badge) {
                badge.textContent = ext.toUpperCase().slice(0, 4);
                badge.className = 'up-ext' + (ext === 'pdf' ? ' pdf' : (ext === 'ppt' || ext === 'pptx') ? ' ppt' : '');
            }
            var nameEl = NVL.$('chosenName');
            if (nameEl) { nameEl.textContent = f.name; nameEl.title = f.name; }
            var sizeEl = NVL.$('chosenSize');
            if (sizeEl) sizeEl.textContent = fmtSize(f.size) + ' / 10 MB';

            var meter = NVL.$('chosenMeter');
            if (meter) {
                meter.style.width = '0%';
                meter.className = f.size > MAX * 0.8 ? 'hot' : '';
            }
            if (wrap) wrap.classList.add('has-file');

            requestAnimationFrame(function () {
                requestAnimationFrame(function () {
                    if (meter) meter.style.width = Math.max(3, f.size / MAX * 100) + '%';
                });
            });
            updateProgress();
        }

        if (input) input.addEventListener('change', handleFile);
        var rmBtn = NVL.$('removeFile');
        if (rmBtn) rmBtn.addEventListener('click', function () { clearFile(); if (input) input.focus(); });

        if (drop) {
            ['dragenter', 'dragover'].forEach(function (ev) {
                drop.addEventListener(ev, function (e) { e.preventDefault(); drop.classList.add('drag'); });
            });
            ['dragleave', 'drop'].forEach(function (ev) {
                drop.addEventListener(ev, function (e) { e.preventDefault(); drop.classList.remove('drag'); });
            });
            drop.addEventListener('drop', function (e) {
                var files = e.dataTransfer && e.dataTransfer.files;
                if (!files || !files.length || !input) return;
                try {
                    var dt = new DataTransfer();
                    dt.items.add(files[0]);
                    input.files = dt.files;
                } catch (ex) {
                    input.files = files;
                }
                handleFile();
            });
        }

        window.addEventListener('dragover', function (e) { e.preventDefault(); });
        window.addEventListener('drop', function (e) { e.preventDefault(); });

        /* Tiến độ điền các trường bắt buộc */
        var form = NVL.$('uplForm');
        var requiredEls = form ? Array.prototype.slice.call(form.querySelectorAll('[required]')) : [];

        function isFilled(el) {
            if (el.type === 'file') return el.files && el.files.length > 0;
            return String(el.value || '').trim() !== '' && (!el.pattern || new RegExp('^(?:' + el.pattern + ')$').test(el.value.trim()));
        }
        function updateProgress() {
            var done = requiredEls.filter(isFilled).length;
            var total = requiredEls.length;
            NVL.setText('progNum', done + '/' + total);
            var b = NVL.$('progBar');
            if (b) {
                b.style.width = (total ? done / total * 100 : 0) + '%';
                b.classList.toggle('done', done === total);
            }
        }
        if (form) {
            form.addEventListener('input', updateProgress);
            form.addEventListener('change', updateProgress);
            updateProgress();

            var submitting = false;
            form.addEventListener('submit', function (e) {
                if (submitting) { e.preventDefault(); return; }
                if (!form.checkValidity()) {
                    e.preventDefault();
                    if (!(input && input.files && input.files.length)) {
                        showError('Vui lòng chọn tệp giáo án để tải lên.');
                    }
                    var firstBad = form.querySelector(':invalid');
                    if (firstBad && firstBad !== input) firstBad.focus();
                    if (firstBad && firstBad.reportValidity) firstBad.reportValidity();
                    return;
                }
                submitting = true;
                var btn = NVL.$('submitBtn');
                if (btn) {
                    btn.classList.add('loading');
                    btn.setAttribute('aria-busy', 'true');
                    NVL.setText('submitLabel', 'Đang tải lên...');
                    setTimeout(function () { btn.disabled = true; }, 0);
                }
                var pb = NVL.$('navProgress');
                if (pb) pb.classList.add('run');
            });
        }

        window.addEventListener('pageshow', function (e) {
            if (e.persisted) {
                var btn = NVL.$('submitBtn');
                if (btn) {
                    btn.disabled = false;
                    btn.classList.remove('loading');
                    NVL.setText('submitLabel', 'Tải lên');
                }
            }
        });
    };

    /* ==========================================================================
       MODULE 3: TEACHER PLANS LIST (.plans)
       ========================================================================== */
    NVL.initPlanList = function () {
        var root = NVL.$('plansRoot');
        if (!root) return;

        var rows = NVL.setupRowDelays('#plTable tbody tr');
        requestAnimationFrame(function () {
            requestAnimationFrame(function () { root.classList.add('is-ready'); });
        });

        if (!rows.length) return;
        NVL.setupRowClicks(rows);

        /* Huy hiệu hạn chia sẻ */
        document.querySelectorAll('[data-expires]').forEach(function (el) {
            var d = new Date(el.getAttribute('data-expires'));
            if (isNaN(d.getTime())) return;
            var days = Math.ceil((d.getTime() - Date.now()) / 86400000);
            var b = document.createElement('span');
            if (days < 0) { b.className = 'pl-badge bad'; b.textContent = 'Đã hết hạn'; }
            else if (days <= 7) { b.className = 'pl-badge warn'; b.textContent = days === 0 ? 'Hết hạn hôm nay' : 'Còn ' + days + ' ngày'; }
            else return;
            b.style.marginLeft = '8px';
            el.appendChild(b);
        });

        /* Tìm kiếm + lọc bộ môn */
        var search = NVL.$('plSearch');
        var deptSel = NVL.$('plDept');
        var shownEl = NVL.$('plShown');
        var totalEl = NVL.$('plTotalPage');
        var noMatch = NVL.$('plNoMatch');

        if (search && deptSel) {
            var depts = {};
            rows.forEach(function (tr) {
                var d = tr.getAttribute('data-dept');
                if (d) depts[d] = true;
                tr.__text = NVL.norm(tr.textContent);
            });
            Object.keys(depts).sort(function (a, b) { return a.localeCompare(b, 'vi'); }).forEach(function (d) {
                var o = document.createElement('option');
                o.value = d; o.textContent = d;
                deptSel.appendChild(o);
            });
            if (totalEl) totalEl.textContent = rows.length;

            function apply() {
                var q = NVL.norm(search.value.trim());
                var dept = deptSel.value;
                var shown = 0;
                rows.forEach(function (tr) {
                    var ok = (!q || tr.__text.indexOf(q) !== -1) && (!dept || tr.getAttribute('data-dept') === dept);
                    tr.style.display = ok ? '' : 'none';
                    tr.classList.remove('row-in');
                    if (ok) shown++;
                });
                if (shownEl) shownEl.textContent = shown;
                if (noMatch) noMatch.classList.toggle('show', shown === 0);
            }
            search.addEventListener('input', apply);
            deptSel.addEventListener('change', apply);
            apply();
        }
    };

    /* ==========================================================================
       MODULE 4: ADMIN PLANS MANAGEMENT (.adm)
       ========================================================================== */
    NVL.initAdminPlans = function () {
        var root = NVL.$('admRoot');
        if (!root) return;

        var rows = NVL.setupRowDelays('#adTable tbody tr');
        requestAnimationFrame(function () {
            requestAnimationFrame(function () { root.classList.add('is-ready'); });
        });

        NVL.setupAvatars('.ad-owner', '.ad-avatar', '.nm');
        NVL.setupRowClicks(rows);

        var form = NVL.$('filterForm');
        var fSearch = NVL.$('fSearch'), fDept = NVL.$('fDept'), fYear = NVL.$('fYear'), fSem = NVL.$('fSem'), fFav = NVL.$('fFav');

        function submitForm() {
            root.classList.add('is-loading');
            var pb = NVL.$('navProgress'); if (pb) pb.classList.add('run');
            form.submit();
        }

        /* Chip bộ lọc đang áp dụng */
        var host = NVL.$('chipHost');
        var items = [];
        var isEn = (document.cookie.indexOf('nvl_lang=en') !== -1 || localStorage.getItem('nvl_lang') === 'en');
        if (fSearch && fSearch.value.trim()) items.push({ el: fSearch, text: (isEn ? 'Keyword: ' : 'Từ khoá: ') + fSearch.value.trim() });
        if (fDept && fDept.value) items.push({ el: fDept, text: fDept.options[fDept.selectedIndex].text });
        if (fYear && fYear.value && fYear.value.trim()) {
            var prefix = isEn ? 'School year ' : 'Năm học ';
            var yText = (fYear.tagName === 'SELECT') ? (prefix + fYear.options[fYear.selectedIndex].text) : (prefix + fYear.value.trim());
            items.push({ el: fYear, text: yText });
        }
        if (fSem && fSem.value) items.push({ el: fSem, text: fSem.options[fSem.selectedIndex].text });
        if (fFav && fFav.value) items.push({ el: fFav, text: fFav.options[fFav.selectedIndex].text });

        if (host && items.length) {
            items.forEach(function (it) {
                var chip = document.createElement('span');
                chip.className = 'ad-chip';
                var label = document.createElement('span');
                label.textContent = it.text;
                var btn = document.createElement('button');
                btn.type = 'button';
                btn.setAttribute('aria-label', (isEn ? 'Remove filter ' : 'Bỏ bộ lọc ') + it.text);
                btn.innerHTML = '<svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.6" stroke-linecap="round"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg>';
                btn.addEventListener('click', function () { it.el.value = ''; submitForm(); });
                chip.appendChild(label);
                chip.appendChild(btn);
                host.appendChild(chip);
            });
            var bar = NVL.$('activeBar');
            if (bar) bar.classList.add('show');
        }

        if (fDept) fDept.addEventListener('change', submitForm);
        if (fYear) fYear.addEventListener('change', submitForm);
        if (fSem) fSem.addEventListener('change', submitForm);
        if (fFav) fFav.addEventListener('change', submitForm);
        if (form) {
            form.addEventListener('submit', function () {
                root.classList.add('is-loading');
                var pb = NVL.$('navProgress'); if (pb) pb.classList.add('run');
            });
        }
    };

    /* ==========================================================================
       MODULE 5: ADMIN USERS MANAGEMENT (.usr)
       ========================================================================== */
    NVL.initAdminUsers = function () {
        var root = NVL.$('usrRoot');
        if (!root) return;

        var rows = NVL.setupRowDelays('#usTable tbody tr');
        requestAnimationFrame(function () {
            requestAnimationFrame(function () { root.classList.add('is-ready'); });
        });

        NVL.setupAvatars('.us-person', '.us-avatar', '.nm');
        NVL.setupRowClicks(rows, 'a[href].us-btn');

        /* Duyệt nhanh tài khoản */
        document.querySelectorAll('.approve-form').forEach(function (f) {
            f.addEventListener('submit', function (e) {
                if (f.__busy) { e.preventDefault(); return; }
                f.__busy = true;
                var b = f.querySelector('button');
                if (b) {
                    var sp = b.querySelector('span');
                    if (sp) sp.textContent = 'Đang duyệt...';
                    setTimeout(function () { b.disabled = true; }, 0);
                }
            });
        });

        var form = NVL.$('filterForm');
        var fSearch = NVL.$('fSearch'), fDept = NVL.$('fDept'), fCampus = NVL.$('fCampus');

        function submitForm() {
            root.classList.add('is-loading');
            var pb = NVL.$('navProgress'); if (pb) pb.classList.add('run');
            form.submit();
        }

        var host = NVL.$('chipHost');
        var items = [];
        if (fSearch && fSearch.value.trim()) items.push({ el: fSearch, text: 'Từ khoá: ' + fSearch.value.trim() });
        if (fDept && fDept.value) items.push({ el: fDept, text: fDept.options[fDept.selectedIndex].text });
        if (fCampus && fCampus.value) items.push({ el: fCampus, text: fCampus.options[fCampus.selectedIndex].text });

        if (host && items.length) {
            items.forEach(function (it) {
                var chip = document.createElement('span');
                chip.className = 'us-chip';
                var label = document.createElement('span');
                label.textContent = it.text;
                var btn = document.createElement('button');
                btn.type = 'button';
                btn.setAttribute('aria-label', 'Bỏ bộ lọc ' + it.text);
                btn.innerHTML = '<svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.6" stroke-linecap="round"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg>';
                btn.addEventListener('click', function () { it.el.value = ''; submitForm(); });
                chip.appendChild(label);
                chip.appendChild(btn);
                host.appendChild(chip);
            });
            var bar = NVL.$('activeBar');
            if (bar) bar.classList.add('show');
        }

        if (fDept) fDept.addEventListener('change', submitForm);
        if (fCampus) fCampus.addEventListener('change', submitForm);
        if (form) {
            form.addEventListener('submit', function () {
                root.classList.add('is-loading');
                var pb = NVL.$('navProgress'); if (pb) pb.classList.add('run');
            });
        }
    };

    /* ==========================================================================
       MODULE 6: ADMIN ROLES & PERMISSIONS (.rol)
       ========================================================================== */
    NVL.initAdminRoles = function () {
        var root = NVL.$('rolRoot');
        if (!root) return;

        requestAnimationFrame(function () {
            requestAnimationFrame(function () { root.classList.add('is-ready'); });
        });

        /* Thu gọn danh sách quyền trên thẻ */
        var LIMIT = 6;
        document.querySelectorAll('[data-perms]').forEach(function (box) {
            var tags = Array.prototype.slice.call(box.querySelectorAll('.rl-tag'));
            if (tags.length <= LIMIT) return;
            tags.slice(LIMIT).forEach(function (t) { t.classList.add('hide'); });
            var more = document.createElement('button');
            more.type = 'button';
            more.className = 'rl-more';
            var extra = tags.length - LIMIT;
            more.textContent = '+' + extra + ' quyền khác';
            var open = false;
            more.addEventListener('click', function () {
                open = !open;
                tags.slice(LIMIT).forEach(function (t) { t.classList.toggle('hide', !open); });
                more.textContent = open ? 'Thu gọn' : '+' + extra + ' quyền khác';
            });
            box.appendChild(more);
        });

        /* Tìm kiếm vai trò */
        var cards = Array.prototype.slice.call(document.querySelectorAll('#roleGrid .rl-card'));
        var rs = NVL.$('roleSearch');
        if (rs && cards.length) {
            cards.forEach(function (c) {
                var nameText = c.querySelector('.rl-name') ? c.querySelector('.rl-name').textContent : '';
                var codeText = c.querySelector('.rl-code') ? c.querySelector('.rl-code').textContent : '';
                c.__text = NVL.norm(nameText + ' ' + codeText);
            });
            var shownEl = NVL.$('roleShown');
            var noMatch = NVL.$('roleNoMatch');

            function applyRoles() {
                var q = NVL.norm(rs.value.trim());
                var n = 0;
                cards.forEach(function (c) {
                    var ok = !q || c.__text.indexOf(q) !== -1;
                    c.style.display = ok ? '' : 'none';
                    c.classList.remove('reveal');
                    if (ok) n++;
                });
                if (shownEl) shownEl.textContent = n;
                if (noMatch) noMatch.classList.toggle('hidden', n !== 0);
            }
            rs.addEventListener('input', applyRoles);
            applyRoles();
        }

        /* Hộp thoại dialog modal */
        function openDialog(d) {
            if (!d) return;
            d.classList.remove('closing');
            if (typeof d.showModal === 'function') d.showModal(); else d.setAttribute('open', '');
            var first = d.querySelector('input[type="text"]');
            if (first) first.focus();
        }
        function finishClose(d) {
            d.classList.remove('closing');
            if (typeof d.close === 'function') d.close(); else d.removeAttribute('open');
            var f = d.querySelector('form');
            if (f && !f.__busy) { f.reset(); if (d.__count) d.__count(); }
        }
        function closeDialog(d) {
            if (!d || !d.open) return;
            if (NVL.reduceMotion) { finishClose(d); return; }
            d.classList.add('closing');
            setTimeout(function () { finishClose(d); }, 160);
        }

        document.addEventListener('click', function (e) {
            var opener = e.target.closest ? e.target.closest('[data-open]') : null;
            if (opener) { openDialog(NVL.$(opener.getAttribute('data-open'))); return; }
            var closer = e.target.closest ? e.target.closest('[data-close]') : null;
            if (closer) { closeDialog(closer.closest('dialog')); return; }
            if (e.target.tagName === 'DIALOG' && e.target.classList.contains('rl-dialog')) closeDialog(e.target);
        });

        document.querySelectorAll('dialog.rl-dialog').forEach(function (d) {
            d.addEventListener('cancel', function (e) { e.preventDefault(); closeDialog(d); });
        });

        /* Công tắc quyền: đếm và bật/tắt toàn bộ */
        document.querySelectorAll('dialog.rl-dialog').forEach(function (d) {
            var boxes = Array.prototype.slice.call(d.querySelectorAll('.rl-perm input'));
            var totalEl = d.querySelector('.picked-t');
            if (totalEl) totalEl.textContent = boxes.length;
            var numEl = d.querySelector('.picked-n');

            function count() {
                if (numEl) numEl.textContent = boxes.filter(function (b) { return b.checked; }).length;
            }
            d.__count = count;

            var btnAll = d.querySelector('[data-all]');
            if (btnAll) btnAll.addEventListener('click', function () { boxes.forEach(function (b) { b.checked = true; }); count(); });
            var btnNone = d.querySelector('[data-none]');
            if (btnNone) btnNone.addEventListener('click', function () { boxes.forEach(function (b) { b.checked = false; }); count(); });

            boxes.forEach(function (b) { b.addEventListener('change', count); });
            count();
        });

        /* Mã vai trò: tự động bỏ dấu tiếng Việt, viết hoa và đổi khoảng trắng thành gạch dưới */
        var code = NVL.$('roleCode');
        if (code) {
            function sanitizeCode() {
                var raw = code.value || '';
                var cleaned = NVL.norm(raw)
                    .toUpperCase()
                    .replace(/[^A-Z0-9_]/g, '_')
                    .replace(/_+/g, '_');
                if (cleaned !== raw) {
                    var pos = code.selectionStart;
                    code.value = cleaned;
                    if (pos !== null) {
                        try { code.setSelectionRange(pos, pos); } catch (e) {}
                    }
                }
            }
            code.addEventListener('input', sanitizeCode);
            code.addEventListener('blur', function () {
                var v = NVL.norm(code.value).toUpperCase().replace(/[^A-Z0-9_]/g, '_').replace(/^_+|_+$/g, '');
                code.value = v;
            });
        }

        /* Gửi biểu mẫu: chống bấm đúp và ngăn chặn treo "Đang lưu..." khi validation lỗi */
        document.querySelectorAll('form.role-form').forEach(function (f) {
            var btn = f.querySelector('button[type="submit"]');
            if (btn && !btn.dataset.originalText) {
                btn.dataset.originalText = btn.textContent.trim();
            }

            f.addEventListener('submit', function (e) {
                // Tự động chuẩn hoá mã vai trò trước khi kiểm tra validation
                var codeEl = f.querySelector('#roleCode');
                if (codeEl && codeEl.value) {
                    codeEl.value = NVL.norm(codeEl.value).toUpperCase().replace(/[^A-Z0-9_]/g, '_').replace(/^_+|_+$/g, '');
                }

                if (e.defaultPrevented) return;
                if (f.__busy) { e.preventDefault(); return; }

                // Kiểm tra sau khi tất cả các listener validation (như form-validation.js) chạy xong
                setTimeout(function () {
                    if (e.defaultPrevented) {
                        f.__busy = false;
                        if (btn) {
                            btn.disabled = false;
                            btn.textContent = btn.dataset.originalText || (f.getAttribute('action') === '/admin/roles' ? 'Tạo vai trò' : 'Lưu');
                        }
                        var pb = NVL.$('navProgress'); if (pb) pb.classList.remove('run');
                        return;
                    }
                    f.__busy = true;
                    if (btn) {
                        btn.textContent = 'Đang lưu...';
                        btn.disabled = true;
                    }
                    var pb = NVL.$('navProgress'); if (pb) pb.classList.add('run');
                }, 0);
            });
        });

        window.addEventListener('pageshow', function (e) {
            if (!e.persisted) return;
            document.querySelectorAll('form.role-form').forEach(function (f) {
                f.__busy = false;
                var btn = f.querySelector('button[type="submit"]');
                if (btn) {
                    btn.disabled = false;
                    btn.textContent = btn.dataset.originalText || (f.getAttribute('action') === '/admin/roles' ? 'Tạo vai trò' : 'Lưu thay đổi');
                }
            });
        });
    };

    /**
     * Hỗ trợ tự động tính toán và hiển thị số tiết học 
     * Khoảng tiết: Từ tiết ... Đến tiết ...
     */
    NVL.setupPeriodHelper = function (startId, endId, boxId, textId) {
        var startEl = NVL.$(startId);
        var endEl = NVL.$(endId);
        var boxEl = NVL.$(boxId);
        var textEl = NVL.$(textId);
        if (!startEl || !endEl || !boxEl || !textEl) return;

        function update() {
            var sVal = (startEl.value || '').trim();
            var eVal = (endEl.value || '').trim();

            if (!sVal && !eVal) {
                boxEl.style.display = 'none';
                endEl.setCustomValidity('');
                return;
            }

            var s = parseInt(sVal, 10);
            var e = parseInt(eVal, 10);

            boxEl.style.display = 'flex';

            if (!isNaN(s) && isNaN(e)) {
                // Giáo án 1 tiết
                boxEl.style.background = '#f0fdf4';
                boxEl.style.borderColor = '#bbf7d0';
                boxEl.style.color = '#15803d';
                textEl.innerHTML = '<strong>Giáo án 1 tiết:</strong> Tiết ' + s + ' (1 tiết)';
                endEl.setCustomValidity('');
            } else if (!isNaN(s) && !isNaN(e)) {
                if (e < s) {
                    boxEl.style.background = '#fef2f2';
                    boxEl.style.borderColor = '#fecaca';
                    boxEl.style.color = '#b91c1c';
                    textEl.innerHTML = '<strong>Lỗi:</strong> Đến tiết (' + e + ') không được nhỏ hơn Từ tiết (' + s + ').';
                    endEl.setCustomValidity('Đến tiết phải lớn hơn hoặc bằng Từ tiết.');
                } else if (e === s) {
                    boxEl.style.background = '#f0fdf4';
                    boxEl.style.borderColor = '#bbf7d0';
                    boxEl.style.color = '#15803d';
                    textEl.innerHTML = '<strong>Giáo án 1 tiết:</strong> Tiết ' + s + ' (1 tiết)';
                    endEl.setCustomValidity('');
                } else {
                    var count = e - s + 1;
                    boxEl.style.background = '#eff6ff';
                    boxEl.style.borderColor = '#bfdbfe';
                    boxEl.style.color = '#1e40af';
                    textEl.innerHTML = '<strong>Giáo án ' + count + ' tiết:</strong> Tiết ' + s + ' – ' + e + ' (' + count + ' tiết)';
                    endEl.setCustomValidity('');
                }
            } else if (isNaN(s) && !isNaN(e)) {
                boxEl.style.background = '#fffbeb';
                boxEl.style.borderColor = '#fde68a';
                boxEl.style.color = '#b45309';
                textEl.innerHTML = 'Vui lòng nhập <strong>Từ tiết</strong> trước khi nhập Đến tiết.';
                endEl.setCustomValidity('');
            }
        }

        startEl.addEventListener('input', update);
        endEl.addEventListener('input', update);
        startEl.addEventListener('change', update);
        endEl.addEventListener('change', update);
        update();
    };

    /* ==========================================================================
       AUTO INITIALIZATION
       ========================================================================== */
    NVL.autoInit = function () {
        NVL.setupPageTransitions();
        NVL.setupPeriodHelper('startPeriod', 'endPeriod', 'periodHelperBox', 'periodHelperText');
        NVL.setupPeriodHelper('startPeriod', 'endPeriod', 'periodHelperBoxEdit', 'periodHelperTextEdit');
        if (NVL.$('uplRoot')) NVL.initPlanUpload();
        if (NVL.$('plansRoot')) NVL.initPlanList();
        if (NVL.$('admRoot')) NVL.initAdminPlans();
        if (NVL.$('usrRoot')) NVL.initAdminUsers();
        if (NVL.$('rolRoot')) NVL.initAdminRoles();
    };

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', NVL.autoInit);
    } else {
        NVL.autoInit();
    }

})(window);
