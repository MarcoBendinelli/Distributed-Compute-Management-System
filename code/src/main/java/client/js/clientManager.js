(function(){

    let personalUsername,newCompression,taskDetails,newSum,newFormat
    let pageOrchestrator = new PageOrchestrator();

    //on load or reload of windows check if username is defined if not load login page
    //if defined start page orchestrator(responsible for updating a managing elements)
    window.addEventListener("load", () => {
        if(sessionStorage.getItem("username") == null){
            window.location.href = "login.html";
        } else{
            pageOrchestrator.start();
            pageOrchestrator.refresh();
        }
    }, false);

    //represent the Welcome back element and is called when page orchestrator starts the first time
    function PersonalUsername(_username, usernameContainer){
        this.username = _username;
        this.show = function(){
            usernameContainer.textContent = "Welcome back, " + this.username;
        }
    }

    //represent the form to ask for a new formatting action
    function NewFormat(username,btnSendTask,directoryInput){
        this.username = username;
        this.btnSendTask = btnSendTask;
        this.directoryInput = directoryInput;
        this.btnSendTask.addEventListener("click", (e) =>{
            let form = e.target.closest("form");
            form.directory.value = directoryInput.value;
            form.sender.value = username;
            let url = "http://" + http_server_address + "/formatService"
            makeFormCall("POST", url,form,
                function (req) {
                    if (req.readyState === XMLHttpRequest.DONE) {
                        let message = req.responseText;
                        switch (req.status) {
                            case 200: //ok
                                document.getElementById("messageBox").className = "text-success"
                                document.getElementById("messageBox").textContent = "Format Sent";
                                pageOrchestrator.update();
                                break;
                            case 400: // bad request
                                document.getElementById("messageBox").className = "text-danger"
                                document.getElementById("messageBox").textContent = message;
                                break;
                        }
                    }
                }
            ,false);


        },false);
    }

    //represent the form to ask for a new sum action
    function NewSum(username,btnSendTask,directoryInput){
        this.username = username;
        this.btnSendTask = btnSendTask;
        this.directoryInput = directoryInput;
        this.btnSendTask.addEventListener("click", (e) =>{
            let form = e.target.closest("form");
            let url = "http://" + http_server_address + "/sumService"
            let task = {};
            task.sender = username;
            task.num1 = form.num1.value;
            task.num2 = form.num2.value;
            task.resultDirectory = directoryInput.value;
            makeCall("POST", url,JSON.stringify(task),
                function (req) {
                    if (req.readyState === XMLHttpRequest.DONE) {
                        let message = req.responseText;
                        switch (req.status) {
                            case 200: //ok
                                document.getElementById("messageBox").textContent = "Sum Sent";
                                pageOrchestrator.update();
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
        },false);

    }

    //represent the form to ask for a new compression action
    function NewCompression(username,btnSendTask,directoryInput){
        this.username = username;
        this.btnSendTask = btnSendTask;
        this.directoryInput = directoryInput;
        this.btnSendTask.addEventListener("click", (e) =>{
            let form = e.target.closest("form");
            let url = "http://" + http_server_address + "/compressService"
            form.directory.value = directoryInput.value;
            form.sender.value = username;
            makeFormCall("POST", url,form,
                function (req) {
                    if (req.readyState === XMLHttpRequest.DONE) {
                        let message = req.responseText;
                        switch (req.status) {
                            case 200: //ok
                                document.getElementById("messageBox").className = "text-success"
                                document.getElementById("messageBox").textContent = "Compression Sent";
                                pageOrchestrator.update();
                                break;
                            case 400: // bad request
                                document.getElementById("messageBox").className = "text-danger"
                                document.getElementById("messageBox").textContent = message;
                                break;
                        }
                    }
                }
            ,false);
        }, false);
    }

    //represent the table containing task data
    //it queries the server to dynamically build the table
    function TaskDetails(username,taskContainerBody,btnCheck){
        this.username = username;
        this.btnCheck = btnCheck;
        this.btnCheck.addEventListener("click", () =>{
            this.updateTaskStatus();
        });

        //function called to ask the server about the task data to display
        this.updateTaskStatus = function () {
            let url = "http://" + http_server_address + "/compressService?user=" + this.username;
            let self = this;
            makeCall("GET", url, null,
                function(req) {
                    if (req.readyState === XMLHttpRequest.DONE) {
                        let message = req.responseText;
                        switch (req.status) {
                            case 200:
                                let data = JSON.parse(message);//list of task entities
                                self.loadTasks(data)
                                break;
                            case 400: // bad request
                                document.getElementById("messageBox").className = "text-danger"
                                document.getElementById("messageBox").textContent = message;
                                break;
                        }
                    }
                }
            );
        }
        //function called to dynamically build the task data table starting from a list of task entity
        this.loadTasks = function(tasks){
            this.taskContainer = document.getElementById("id_taskContainerBody");
            this.taskContainer.innerHTML="";
            if(tasks==null){
                return;//if no task to show
            }
            let self = this;
            let row,col,id,type,complete;
            tasks.forEach(function (task){
                row = document.createElement("tr");
                Object.entries(task).forEach(function (el){
                    col = document.createElement("th");
                    id=document.createElement("p")
                    id.textContent = el[1].toString();
                    col.appendChild(id);
                    row.appendChild(col);
                })
                self.taskContainer.appendChild(row);
            });
        }
    }

    //main manager of the page
    //called when the page is first loaded to instantiate each element handler
    function PageOrchestrator() {
        let interval;
        this.start = function(){
            personalUsername = new PersonalUsername(sessionStorage.getItem('username'),
                document.getElementById("usernameContainer"));
            newCompression = new NewCompression(
                sessionStorage.getItem('username'),
                document.getElementById("compressButton"),
                document.getElementById("id_result_directory")
            );
            taskDetails = new TaskDetails(
                sessionStorage.getItem('username'),
                document.getElementById("id_taskContainerBody"),
                document.getElementById("btnCheck")
            );
            newSum = new NewSum(
                sessionStorage.getItem('username'),
                document.getElementById("sumButton"),
                document.getElementById("id_result_directory")
            )
            newFormat = new NewFormat(
                sessionStorage.getItem('username'),
                document.getElementById("formatButton"),
                document.getElementById("id_result_directory")
            )
            this.startAutoUpdate()
        }

        this.refresh = function(){
            personalUsername.show();
            taskDetails.updateTaskStatus();
        }

        this.startAutoUpdate = function (){
            interval = setInterval(this.update,1000);//repeat update every N ms
        }

        this.update = function(){
            taskDetails.updateTaskStatus();
        }
    }
})();