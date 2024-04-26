/**
 * Index page management
 */
(function() { // avoid variables ending up in the global scope

    document.getElementById("loginButton").addEventListener('click', (e) => {
        let form = e.target.closest("form");
        let url = "http://" + http_server_address + "/userLogin?user=" + form.username.value +"&password=" + form.password.value;
        if (form.checkValidity()) {
            makeCall("GET", url, null,
                function(req) {
                    if (req.readyState === XMLHttpRequest.DONE) {
                        let message = req.responseText;
                        switch (req.status) {
                            case 200:
                                sessionStorage.setItem('username', form.username.value);
                                window.location.href = "client.html";
                                break;
                            case 400: // bad request
                                document.getElementById("messageBox").textContent = message;
                                break;
                            case 401: // unauthorized
                                document.getElementById("messageBox").textContent = "Invalid Credentials";
                                break;
                        }
                    }
                }
            );
        } else {
            form.reportValidity();
        }
    });
    document.getElementById("registerButton").addEventListener('click', (e) => {
        let form = e.target.closest("form");
        let url = "http://" + http_server_address + "/userLogin?user=" + form.username.value +"&password=" + form.password.value;
        let elements = "username=" + form.username.value + "&password="+ form.password.value;
        if (form.checkValidity()) {
            makeCall("POST", url, elements,
                function(req) {
                    if (req.readyState === XMLHttpRequest.DONE) {
                        let message = req.responseText;
                        switch (req.status) {
                            case 200:
                                document.getElementById("messageBox").textContent = "Registered Successfully";
                                break;
                            case 400: // bad request
                                document.getElementById("messageBox").textContent = message;
                                break;
                            case 401: // unauthorized
                                document.getElementById("messageBox").textContent = "Invalid Credentials";
                                break;
                        }
                    }
                }
            );
        } else {
            form.reportValidity();
        }
    });
})();