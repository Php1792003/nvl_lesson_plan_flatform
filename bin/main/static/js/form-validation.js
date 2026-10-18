/**
 * Validate form phía client. Kích hoạt cho form có thuộc tính data-validate.
 * Quy ước: thông báo lỗi nằm ở phần tử có id "<id-trường>-error".
 */
(function () {
    'use strict';

    var DEFAULT_REQUIRED = 'Vui lòng điền thông tin này.';

    function getFields(form) {
        return Array.prototype.slice.call(form.querySelectorAll('input, select, textarea'))
            .filter(function (f) {
                return f.type !== 'hidden' && f.type !== 'submit' && f.type !== 'button' && !f.disabled;
            });
    }

    function errorBox(field) {
        var id = field.id + '-error';
        var box = document.getElementById(id);
        if (!box) {
            box = document.createElement('p');
            box.id = id;
            box.className = 'field-error';
            box.setAttribute('role', 'alert');
            box.hidden = true;
            var group = field.closest('.form-group') || field.parentNode;
            group.appendChild(box);
        }
        return box;
    }

    /** Trả về thông báo lỗi, hoặc chuỗi rỗng nếu hợp lệ. */
    function check(form, field) {
        var d = field.dataset;
        var isPassword = field.type === 'password';
        var val = isPassword ? field.value : String(field.value || '').trim();

        if (field.type === 'checkbox') {
            if (field.required && !field.checked) return d.msgRequired || DEFAULT_REQUIRED;
            return '';
        }
        if (field.required && val === '') return d.msgRequired || DEFAULT_REQUIRED;
        if (val === '') return '';

        if (d.minlength && val.length < Number(d.minlength)) {
            return d.msgMinlength || ('Cần ít nhất ' + d.minlength + ' ký tự.');
        }
        if (d.maxlength && val.length > Number(d.maxlength)) {
            return d.msgMaxlength || ('Không quá ' + d.maxlength + ' ký tự.');
        }
        if (d.pattern) {
            var re;
            try { re = new RegExp(d.pattern, 'u'); } catch (e) { re = null; }
            if (re && !re.test(val)) return d.msgPattern || 'Giá trị không hợp lệ.';
        }
        if (d.match) {
            var other = form.elements[d.match];
            if (other && other.value !== field.value) return d.msgMatch || 'Giá trị không khớp.';
        }
        return '';
    }

    function show(field, msg) {
        var box = errorBox(field);
        var group = field.closest('.form-group') || field.closest('.rl-field') || field.parentNode;
        if (msg) {
            box.textContent = msg;
            box.hidden = false;
            box.style.display = '';
            field.setAttribute('aria-invalid', 'true');
            if (group) group.classList.add('has-error');
        } else {
            box.textContent = '';
            box.hidden = true;
            field.setAttribute('aria-invalid', 'false');
            if (group) group.classList.remove('has-error');
        }
    }

    function init(form) {
        form.noValidate = true;
        var fields = getFields(form);
        var touched = {};

        function validate(field) {
            var msg = check(form, field);
            show(field, msg);
            return msg;
        }

        fields.forEach(function (f) {
            f.addEventListener('blur', function () {
                touched[f.id] = true;
                validate(f);
            });
            var live = function () {
                if (touched[f.id] || f.getAttribute('aria-invalid') === 'true') validate(f);
                // Đổi mật khẩu thì kiểm tra lại ô xác nhận đã chạm vào
                fields.forEach(function (o) {
                    if (o.dataset.match === f.name && (touched[o.id] || o.getAttribute('aria-invalid') === 'true')) {
                        validate(o);
                    }
                });
            };
            f.addEventListener('input', live);
            f.addEventListener('change', live);
        });

        form.addEventListener('submit', function (e) {
            var firstBad = null;
            fields.forEach(function (f) {
                touched[f.id] = true;
                if (validate(f) && !firstBad) firstBad = f;
            });
            if (firstBad) {
                e.preventDefault();
                firstBad.focus();

                // Phục hồi lại trạng thái nút gửi biểu mẫu nếu đã bị các handler khác đổi thành "Đang lưu..."
                form.__busy = false;
                var submitBtn = form.querySelector('button[type="submit"]');
                if (submitBtn) {
                    submitBtn.disabled = false;
                    if (submitBtn.dataset.originalText) {
                        submitBtn.textContent = submitBtn.dataset.originalText;
                    }
                }
                var pb = document.getElementById('navProgress');
                if (pb) pb.classList.remove('run');
            }
        });

        // Lỗi do server trả về: focus vào trường lỗi đầu tiên
        var serverBad = form.querySelector('[aria-invalid="true"]');
        if (serverBad) serverBad.focus();
    }

    function boot() {
        document.querySelectorAll('form[data-validate]').forEach(init);
    }

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', boot);
    } else {
        boot();
    }
})();
