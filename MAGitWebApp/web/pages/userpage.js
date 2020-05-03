var USERS_INFO_URL = buildUrlWithContextPath("usersinfo");
var FORK_URL = buildUrlWithContextPath("fork");
var ACTIVATE_REPOSITORY_URL = buildUrlWithContextPath("activaterepo")
var NOTIFICATIONS_INFO_URL = buildUrlWithContextPath("notificationsinfo");
var REPOSITORIES_INFO_URL = buildUrlWithContextPath("repositoriesinfo");
var LOGOUT_URL = buildUrlWithContextPath("logout");
var UPLOAD_URL = buildUrlWithContextPath("pages/upload");

var selectedUser = null;
var selectedMyRepo = null;
var selectedUserRepo = null;
var lastReadIdx = -1;

function appendToNotificationArea(entries) {
    $.each(entries || [], appendNotification);

    // handle the scroller to auto scroll to the end of the notification area
    var scroller = $("#notifications_area");
    var height = scroller[0].scrollHeight - $(scroller).height();
    $(scroller).stop().animate({ scrollTop: height }, "slow");
}

function appendNotification(index, entry){
    var entryElement = createNotificationEntry(entry);
    $("#notifications_area").append(entryElement).append("<br>");
    lastReadIdx++;
}

function createNotificationEntry (entry){
    return $("<span>").append(entry.date + " - " + entry.message);
}

function refreshNotificationsList() {
    $.ajax({
        url: NOTIFICATIONS_INFO_URL,
        timeout: 2000,
        dataType: 'json',
        data: {"lastReadIdx": lastReadIdx},
        error: function() {
            console.error("Failed to submit");
        },
        success: function(notifications) {
            appendToNotificationArea(notifications);
        }
    });
}

function escape_id(id) {
    return id.toString().replace(/ /g, '_').replace(/\\/g, '_').replace(/\./g, '_');
}

function populateReposTables(repos) {
    var myTable = $("#my_repos_table");
    repos.myRepos.forEach(function(repo) {
        var reponame1 = escape_id(repo.name);
        var row = $('#my_repo_' + reponame1);
        if (row.length === 0) {
            myTable.append('<tr id="my_repo_' + reponame1 + '"></tr>');
            row = $('#my_repo_' + reponame1);
        }
        row.html("<td>" + repo.name + "</td><td>" + repo.activeBranch + "</td>"
            + "</td><td>" + repo.numberOfBranches + "</td><td>" + repo.dateOfLastCommit + "</td><td>" + repo.lastCommitMessage + "</td>");

        row[0].classList = ["repo-row" + (repo.name === selectedMyRepo ? "-selected" : "")];
        row[0].onclick = function() {
            var selectedRepoRow;
            if (selectedMyRepo !== null) {
                var rep1 = escape_id(selectedMyRepo);
                selectedRepoRow = $('#my_repo_' + rep1);
                var cls = selectedRepoRow[0].classList[0];
                selectedRepoRow[0].classList = [cls.substr(0, (cls.length - "-selected".length))];
            }
            selectedMyRepo = repo.name;
            row[0].classList = ["repo-row-selected"];
        };
    })


    var selctedUserTable = $("#selected_user_repos_table");
    repos.selectedUserRepos.forEach(function(repo) {
        var reponame2 = escape_id(repo.name);
        var row = $('#selected_user_repo_' + reponame2);
        if (row.length === 0) {
            selctedUserTable.append('<tr id="selected_user_repo_' + reponame2 + '"></tr>');
            row = $('#selected_user_repo_' + reponame2);
        }
        row.html("<td>" + repo.name + "</td><td>" + repo.activeBranch + "</td>"
            + "</td><td>" + repo.numberOfBranches + "</td><td>" + repo.dateOfLastCommit + "</td><td>" + repo.lastCommitMessage + "</td>");

        row[0].classList = ["repo-row" + (repo.name === selectedUserRepo ? "-selected" : "")];
        row[0].onclick = function() {
            if (selectedUserRepo !== null) {
                var rep2 = escape_id(selectedUserRepo);
                var selectedRepoRow = $('#selected_user_repo_' + rep2);
                var cls2 = selectedRepoRow[0].classList[0];
                selectedRepoRow[0].classList = [cls2.substr(0, (cls2.length - "-selected".length))];
            }
            selectedUserRepo = repo.name;
            row[0].classList = ["repo-row-selected"];
        };
    })
}

function refreshRepositoriesTables() {
    $.ajax({
        url: REPOSITORIES_INFO_URL,
        timeout: 2000,
        data: {"name": selectedUser},
        error: function() {
            console.error("Failed to submit");
        },
        success: function(repositories) {
            populateReposTables(repositories);
        }
    });
}

function populateUsersTable(users, curr_user) {
    var table = $("#users_table");
    users.forEach(function(user) {
        if (user.name !== curr_user) {
            var row = $('#user_' + user.name);
            if (row.length === 0) {
                table.append('<tr id="user_' + user.name + '"></tr>');
                row = $('#user_' + user.name);
            }
            row.html("<td>" + (user.isOnline ? "Online" : "Offline") +
            "</td><td>" + user.name + "</td>");
            row[0].classList = ["user-row" + (user.isOnline ? "-online" : "-offline") + (user.name === selectedUser ? "-selected" : "")];
            row[0].onclick = function() {
                var table = $("#selected_user_repos_table");
                table[0].tBodies[0].innerText = "";
                if (selectedUser !== null) {
                    var selectedUserRow = $('#user_' + selectedUser);
                    var cls = selectedUserRow[0].classList[0];
                    selectedUserRow[0].classList = [cls.substr(0, (cls.length - "-selected".length))];
                }
                selectedUser = user.name;
                row[0].classList = ["user-row" + (user.isOnline ? "-online-selected" : "-offline-selected")];
            };
        }
    })
}

function refreshUsersList() {
    $.ajax({
        url: USERS_INFO_URL,
        timeout: 2000,
        // data: "",
        error: function(err) {
            console.error("Failed to submit: " + err);
            if (err.readyState === 4) {
                window.location = "signup/signup.html";
            }
        },
        success: function(user) {
            console.debug("got user:" + user.myUsername);
            $("#username").text(user.myUsername);
            populateUsersTable(user.users, user.myUsername);
        }
    });
}

function showDialog(title, status, msg) {
    var dialog = $("#dialog");
    $("#dialog-header").text(title);
    $("#dialog-body")[0].innerHTML = '<div class="alert alert-' + status + '" role="alert">' + msg + '</div>';
    dialog.modal('show');
}


$(function() { // onload...do
    refreshUsersList();
    refreshRepositoriesTables();
    setInterval(refreshUsersList, 2000);
    setInterval(refreshRepositoriesTables, 2000);
    setInterval(refreshNotificationsList, 2000);
    var upload_xml_form = document.forms.namedItem("upload_xml_form");
    upload_xml_form.addEventListener("submit", function (ev) {
        var form_data = new FormData(upload_xml_form);
        $.ajax({
            url: UPLOAD_URL,
            timeout: 2000,
            method: "POST",
            dataType: 'json',
            data: form_data,
            processData: false,
            contentType: false,
            error: function (err) {
                console.error("Failed to submit:" + err);
            },
            success: function (response) {
                console.debug("got response:" + response.msg);
                showDialog("Load repository from XML", response.status, response.msg);
                if (response.status === 'success') {
                    upload_xml_form.elements[0].value = "";
                    upload_xml_form.elements[1].checked = false;
                }
            }
        });
        ev.preventDefault();
    }, false)

    $("#open-btn")[0].onclick = function () {
        $.ajax({
            url: ACTIVATE_REPOSITORY_URL,
            timeout: 2000,
            data: {"reponame" : selectedMyRepo},
            dataType: 'json',
            error: function (err) {
                console.error("Failed to submit:" + err);
            },
            success: function(response) {
                console.debug("got response:" + response.msg);
                if (response.status === "success") {
                    window.location.href = "repopage.html";
                } else {
                    showDialog("Open repository", response.status, response.msg);
                }
            }
        });
    }

    $("#fork-btn")[0].onclick = function () {
        $.ajax({
            url: FORK_URL,
            timeout: 2000,
            data: {"selecteduser": selectedUser, "selectedrepo": selectedUserRepo},
            dataType: 'json',
            error: function (err) {
                console.error("Failed to submit:" + err);
            },
            success: function (response) {
                console.debug("got response:" + response.msg);
                showDialog("Fork", response.status, response.msg);
                if (response.status === "success") {
                    selectedUserRepo = null;
                }
            }
        });
    };
}
);