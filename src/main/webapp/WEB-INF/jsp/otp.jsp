<%@ include file="/WEB-INF/jsp/common/taglibs.jsp"%>
<!DOCTYPE html>
<html lang="en">
<head>
<title>Two-Step Verification</title>
<meta name="viewport"
	content="width=device-width, initial-scale=1.0, shrink-to-fit=no">
<link
	href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css"
	rel="stylesheet">

<style>
/* Modern Segmented 6-Digit Field Styling */
.otp-field-container {
	display: flex;
	gap: 10px;
	justify-content: center;
	margin-bottom: 20px;
}

.otp-input {
	width: 45px;
	height: 50px;
	font-size: 1.5rem;
	font-weight: 600;
	text-align: center;
	border: 2px solid #dee2e6;
	border-radius: 8px;
	background-color: #fff;
	transition: all 0.2s ease-in-out;
}

.otp-input:focus {
	border-color: #0d6efd;
	box-shadow: 0 0 0 0.25rem rgba(13, 110, 253, 0.25);
	outline: none;
}
/* Disable native up/down arrows on number inputs */
.otp-input::-webkit-outer-spin-button, .otp-input::-webkit-inner-spin-button
	{
	-webkit-appearance: none;
	margin: 0;
}

.otp-input[type=number] {
	-moz-appearance: textfield;
}

.disabled-link {
	color: #6c757d;
	text-decoration: none;
	pointer-events: none;
	cursor: default;
}
</style>
</head>
<body class="bg-light">
	<div class="container d-flex align-items-center justify-content-center"
		style="min-height: 100vh;">
		<div class="card shadow-sm" style="max-width: 420px; width: 100%;">
			<div class="card-body p-4">
				<h1 class="h4 mb-1 text-center">Enter verification code</h1>

				<p class="text-secondary text-center mb-4">
					<c:if test="${not empty username}">
						<strong><c:out value="${username}" /></strong> - 
                    </c:if>
					We have sent a 6-digit verification code to your registered email
					account.
				</p>

				<c:if test="${param.error == 'true'}">
					<div class="alert alert-danger text-center py-2">Invalid or
						expired code. Try again.</div>
				</c:if>

				<form id="otpForm" method="post"
					action="${pageContext.request.contextPath}/otp">

					<input type="hidden" id="combinedCode" name="code" />

					<div class="mb-3">
						<label
							class="form-label d-block text-center mb-3 fw-medium text-dark">One-time
							code</label>

						<div class="otp-field-container">
							<input type="number" class="otp-input" min="0" max="9"
								inputmode="numeric" pattern="[0-9]*" required autofocus /> <input
								type="number" class="otp-input" min="0" max="9"
								inputmode="numeric" pattern="[0-9]*" required disabled /> <input
								type="number" class="otp-input" min="0" max="9"
								inputmode="numeric" pattern="[0-9]*" required disabled /> <input
								type="number" class="otp-input" min="0" max="9"
								inputmode="numeric" pattern="[0-9]*" required disabled /> <input
								type="number" class="otp-input" min="0" max="9"
								inputmode="numeric" pattern="[0-9]*" required disabled /> <input
								type="number" class="otp-input" min="0" max="9"
								inputmode="numeric" pattern="[0-9]*" required disabled />
						</div>

						<div class="form-text text-center">6 digits, valid for 5
							minutes.</div>
					</div>

					<input type="hidden" name="${_csrf.parameterName}"
						value="${_csrf.token}" />

					<button class="btn btn-primary w-100 py-2 fw-semibold"
						type="submit" id="submitBtn" disabled>Verify</button>
				</form>

				<div class="text-center mt-4" style="font-size: .9rem;">
					<c:if test="${resendSuccess == true}">
						<div class="alert alert-success py-1 small mb-2">A fresh
							verification code has been dispatched!</div>
					</c:if>

					<span id="timerText" class="text-secondary">Didn't receive
						code? Resend in <span id="countdown" class="fw-bold">60</span>s
					</span>

					<form id="resendForm"
						action="${pageContext.request.contextPath}/otp/resend"
						method="POST" class="d-none">
						<input type="hidden" name="${_csrf.parameterName}"
							value="${_csrf.token}" /> <a href="#"
							onclick="document.getElementById('resendForm').submit(); return false;"
							id="resendLink" class="disabled-link">Resend Verification
							Code</a>
					</form>
				</div>

				<p class="text-center text-secondary mt-3 mb-0"
					style="font-size: .9rem;">
					Entered the wrong account? <a
						href="${pageContext.request.contextPath}/login"
						class="text-decoration-none">Back to login</a>
				</p>
			</div>
		</div>
	</div>

	<script>
        document.addEventListener("DOMContentLoaded", function () {
            const inputs = document.querySelectorAll(".otp-input");
            const combinedHiddenInput = document.getElementById("combinedCode");
            const submitBtn = document.getElementById("submitBtn");
            const form = document.getElementById("otpForm");

            // 1. Core Segmented Navigation Operations Engine
            inputs.forEach((input, index) => {
                // Focus handling when user inputs characters
                input.addEventListener("input", (e) => {
                    const currentVal = input.value;
                    if (currentVal.length > 1) {
                        input.value = currentVal.slice(-1); // Lock down input string limit size to 1 character max
                    }

                    if (input.value !== "") {
                        // Advance cursor focus downstream
                        if (index < inputs.length - 1) {
                            inputs[index + 1].removeAttribute("disabled");
                            inputs[index + 1].focus();
                        }
                    }
                    updateCombinedValue();
                });

                // Clear / Shift backward on Delete operations
                input.addEventListener("keydown", (e) => {
                    if (e.key === "Backspace") {
                        if (input.value === "") {
                            if (index > 0) {
                                inputs[index - 1].focus();
                                inputs[index - 1].value = "";
                                updateCombinedValue();
                            }
                        } else {
                            input.value = "";
                            updateCombinedValue();
                        }
                    }
                });
            });

            // Handle standard paste events smoothly across inputs (e.g. copied from email notification layout)
            inputs[0].addEventListener("paste", (e) => {
                e.preventDefault();
                const pasteData = (e.clipboardData || window.clipboardData).getData("text").trim();
                
                if (/^\d{6}$/.test(pasteData)) { // Confirm string sequence structure matches 6 integers explicitly
                    inputs.forEach((input, index) => {
                        input.removeAttribute("disabled");
                        input.value = pasteData[index];
                    });
                    updateCombinedValue();
                    submitBtn.focus();
                }
            });

            function updateCombinedValue() {
                let code = "";
                inputs.forEach(input => code += input.value);
                combinedHiddenInput.value = code;
                
                // Only allow form submission once all 6 boxes are fully populated
                if (code.length === 6) {
                    submitBtn.removeAttribute("disabled");
                } else {
                    submitBtn.setAttribute("disabled", "true");
                }
            }

            // 2. Production Resend Lockout Countdown Timer Engine
            let timeLeft = 60;
            const countdownSpan = document.getElementById("countdown");
            const timerText = document.getElementById("timerText");
            const resendLink = document.getElementById("resendLink");

            const downloadTimer = setInterval(function () {
                timeLeft--;
                countdownSpan.textContent = timeLeft;
                if (timeLeft <= 0) {
                    clearInterval(downloadTimer);
                    document.getElementById("timerText").classList.add("d-none");
                    
                    const formContainer = document.getElementById("resendForm");
                    formContainer.classList.remove("d-none"); // Make the form visible
                    
                    const linkTarget = document.getElementById("resendLink");
                    linkTarget.classList.remove("disabled-link"); // Unlock action state
                }
            }, 1000);
        });
    </script>
</body>
</html>