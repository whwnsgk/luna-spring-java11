var button = document.getElementById("callButton");
var countElement = document.getElementById("count");
var messageElement = document.getElementById("message");

function refreshCount() {
    fetch("/api/luna/count")
        .then(function(response) {
            if (!response.ok) {
                throw new Error("호출 횟수 조회 실패");
            }
            return response.json();
        })
        .then(function(data) {
            countElement.textContent = data.count;
        })
        .catch(function(error) {
            countElement.textContent = "오류";
            messageElement.textContent = "Spring 서버 또는 DB에 연결하지 못했습니다.";
            console.error(error);
        });
}

button.addEventListener("click", function() {
    button.disabled = true;
    messageElement.textContent = "루나가 PostgreSQL을 거쳐 달려오는 중이에요...";

    fetch("/api/luna/call", {
        method: "POST"
    })
        .then(function(response) {
            if (!response.ok) {
                throw new Error("루나 호출 실패");
            }
            return response.json();
        })
        .then(function(data) {
            countElement.textContent = data.count;
            messageElement.textContent = data.message + " (DB 저장 성공)";
        })
        .catch(function(error) {
            messageElement.textContent = "호출에 실패했습니다. 서버 로그와 DB 설정을 확인해주세요.";
            console.error(error);
        })
        .finally(function() {
            button.disabled = false;
        });
});

refreshCount();
