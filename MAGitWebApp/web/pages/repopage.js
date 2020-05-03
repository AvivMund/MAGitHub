var ACTIVE_REPO_INFO_URL = buildUrlWithContextPath("activerepoinfo");
var NOTIFICATIONS_INFO_URL = buildUrlWithContextPath("notificationsinfo");
var CHECKOUT_URL = buildUrlWithContextPath("checkout");
var HEAD_BRANCH_COMMITS_URL = buildUrlWithContextPath("headbranchcommits");
var COMMIT_FILES_URL = buildUrlWithContextPath("commitfiles");
var COMMIT_URL = buildUrlWithContextPath("commit");
var NEW_BRANCH_URL = buildUrlWithContextPath("createbranch");
var PULL_URL = buildUrlWithContextPath("pull");
var PUSH_URL = buildUrlWithContextPath("push");
var PRS_INFO_URL = buildUrlWithContextPath("pullrequestsinfo");
var CREATE_PR_URL = buildUrlWithContextPath("createpullrequest");
var REJECT_URL = buildUrlWithContextPath("reject");
var ACCEPT_URL = buildUrlWithContextPath("accept");
var PR_PAGE_URL = buildUrlWithContextPath("pullrequestpage");

var lastReadIdx = -1;
var selectedBranch = null;
var selectedCommit = null;
var selectedPr = -1;

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
        error: function(err) {
            console.error("Failed to submit: " + err);
            if (err.readyState === 4) {
                window.location = "signup/signup.html";
            }
        },
        success: function(notifications) {
            appendToNotificationArea(notifications);
        }
    });
}

function escape_id(id) {
    return id.toString().replace(/ /g, '_').replace(/\\/g, '_').replace(/\./g, '_');
}

function populateSelectedCommitFilesTable(fileslist) {
    var table = $("#selected_commit_files_table");
    table[0].tBodies[0].innerText = "";
    fileslist.forEach(function(file) {
        var filename = escape_id(file);
        table.append('<tr id="file_' + filename + '"></tr>');
        var row = $('#file_' + filename);
        row.html("<td>" + file + "</td>");
    });
}

function refreshSelectedCommitTable() {
    $.ajax({
        url: COMMIT_FILES_URL,
        timeout: 2000,
        dataType: 'json',
        data: {"selectedcommitsha1": selectedCommit},
        error: function() {
            console.error("Failed to submit");
        },
        success: function(fileslist) {
            populateSelectedCommitFilesTable(fileslist);
        }
    });

}

function populateHeadBranchCommitsTable(commits){
    var table = $("#head_commits_table");
    table[0].tBodies[0].innerText = "";
    commits.forEach(function(commit) {
        table.append('<tr id="commit_' + commit.sha1 + '"></tr>');
        var row = $('#commit_' + commit.sha1);
        var branchesString= "";
        commit.branches.forEach(function (branch) {
           branchesString = branchesString + branch + ", ";
        });
        branchesString = branchesString.substr(0, (branchesString.length - ", ".length));
        row.html("<td>" + commit.sha1 + "</td><td>" + commit.message + "</td><td>" +
            commit.date + "</td><td>" + commit.author + "</td><td>" + branchesString + "</td>");
        row[0].classList = ["commit-row" + (commit === selectedCommit ? "-selected" : "")];
        row[0].onclick = function() {
            var selectedRow;
            if (selectedCommit !== null) {
                selectedRow =  $('#commit_' + selectedCommit);
                var cls = selectedRow[0].classList[0];
                selectedRow[0].classList = [cls.substr(0, (cls.length - "-selected".length))];
            }
            selectedCommit = commit.sha1;
            row[0].classList = ["commit-row-selected"];
            refreshSelectedCommitTable();
        };
    })
}

function populateBranchesTable(head, branches) {
    var prFormShown = $("#pr_dialog")[0].attributes["style"]["value"].match('display: block');
    var table = $("#branches_table");
    var head_id = escape_id(head.name);
    table.append('<tr id="branch_' + head_id + '"></tr>');
    var row = $('#branch_' + head_id);
    row.html("<td>HEAD: " + head.name + "</td>");
    row[0].classList = ["branch-row" + (head.name === selectedBranch ? "-selected" : "")];
    row[0].onclick = function() {
        var selectedRow;
        var selectedBranch_id;
        if (selectedBranch !== null) {
            selectedBranch_id = escape_id(selectedBranch);
            selectedRow = $('#branch_' + selectedBranch_id);
            var cls = selectedRow[0].classList[0];
            selectedRow[0].classList = [cls.substr(0, (cls.length - "-selected".length))];
        }
        selectedBranch = head.name;
        row[0].classList = ["branch-row-selected"];
    };
    var baseSelect = $("#base_branch");
    var targetSelect = $("#target_branch");
    if (prFormShown === null) {
        baseSelect[0].innerText = "";
        baseSelect.append('<option value="">choose</option>');
        targetSelect[0].innerText = "";
        targetSelect.append('<option value="">choose</option>');
        if (head.isRTB) {
            baseSelect.append('<option value="' + head.name + '">' + head.name + '</option>');
            targetSelect.append('<option value="' + head.name + '">' + head.name + '</option>');
        }
    }

    branches.forEach(function(branch) {
        var branch_id = escape_id(branch.name);
        row = $('#branch_' + branch_id);
        if (row.length === 0) {
            table.append('<tr id="branch_' + branch_id + '"></tr>');
        }
        row = $('#branch_' + branch_id);
        row.html("<td>" + branch.name + "</td>");
        row[0].classList = ["branch-row" + (branch.name === selectedBranch ? "-selected" : "")];
        row[0].onclick = function() {
            var selectedRow;
            var selectedBranch_id;
            if (selectedBranch !== null) {
                selectedBranch_id = escape_id(selectedBranch);
                selectedRow =  $('#branch_' + selectedBranch_id);
                var cls2 = selectedRow[0].classList[0];
                selectedRow[0].classList = [cls2.substr(0, (cls2.length - "-selected".length))];
            }
            selectedBranch = branch.name;
            row[0].classList = ["branch-row-selected"];
        };
        if (prFormShown === null) {
            if (branch.isRTB) {
                baseSelect.append('<option value="' + branch.name + '">' + branch.name + '</option>');
                targetSelect.append('<option value="' + branch.name + '">' + branch.name + '</option>');
            }
        }
    })
}

function refreshHeadBranchCommits() {
    $.ajax({
        url: HEAD_BRANCH_COMMITS_URL,
        timeout: 2000,
        //data: "",
        error: function(err) {
            console.error("Failed to submit: " + err);
        },
        success: function(commits) {
            populateHeadBranchCommitsTable(commits);
        }
    });
}

function refreshRepoInfo() {
    $.ajax({
        url: ACTIVE_REPO_INFO_URL,
        timeout: 2000,
        //data: "",
        error: function(err) {
            console.error("Failed to submit: " + err);
        },
        success: function(response) {
            console.debug("got user:" + response.myUsername);
            $("#username").text(response.myUsername);
            $("#reponame").text(response.repoName);
            if (response.remoteName.length > 0) {
                $("#reponame").text(response.repoName + " (Remote name: " + response.remoteName + ", owner: " + response.remoteOwner + ")");
            }
            populateBranchesTable(response.head, response.branches);
        }
    });
}

function showDialog(title, status, msg) {
    var dialog = $("#dialog");
    $("#dialog-header").text(title);
    $("#dialog-body")[0].innerHTML = '<div class="alert alert-' + status + '" role="alert">' + msg + '</div>';
    dialog.modal('show');
}

function populatePullRequestsTable(prs) {
    var table = $("#pr_table");
    table[0].tBodies[0].innerText = "";
    prs.forEach(function(pr) {
        table.append('<tr id="pr_' + pr.id + '"></tr>');
        var row = $('#pr_' + pr.id);
        row.html("<td>" + pr.requestUsername + "</td><td>" + pr.targetBranch + "</td><td>" +
            pr.baseBranch + "</td><td>" + pr.date + "</td><td>" + pr.prStatus + "</td>");
        row[0].classList = ["pr-row" + (pr.id === selectedPr ? "-selected" : "")];
        row[0].onclick = function() {
            var selectedRow;
            if (selectedPr !== -1) {
                selectedRow =  $('#pr_' + selectedPr);
                var cls = selectedRow[0].classList[0];
                selectedRow[0].classList = [cls.substr(0, (cls.length - "-selected".length))];
            }
            selectedPr = pr.id;
            row[0].classList = ["pr-row-selected"];
            //TODO: manage and view the pr
        };
    })
}

function refreshPullRequestList() {
    $.ajax({
        url: PRS_INFO_URL,
        timeout: 2000,
        //data: "",
        error: function(err) {
            console.error("Failed to submit: " + err);
        },
        success: function(prs) {
            populatePullRequestsTable(prs);
        }
    });
}

$(function() { // onload...do
        refreshRepoInfo();
        refreshHeadBranchCommits();
        refreshPullRequestList();
        setInterval(refreshRepoInfo, 2000);
        setInterval(refreshNotificationsList, 2000);
        setInterval(refreshPullRequestList, 2000);

        $("#checkout-btn")[0].onclick = function () {
            $.ajax({
                url: CHECKOUT_URL,
                timeout: 2000,
                data: {"selectedBranch": selectedBranch},
                dataType: 'json',
                error: function (err) {
                    console.error("Failed to submit:" + err);
                },
                success: function (response) {
                    console.debug("got response:" + response.msg);
                    showDialog("Checkout", response.status, response.msg);
                    if (response.status === "success") {
                        selectedBranch = null;
                        refreshHeadBranchCommits();
                    }
                }
            });
        };

        $("#pull-btn")[0].onclick = function () {
            $.ajax({
                url: PULL_URL,
                timeout: 2000,
                //data: ,
                dataType: 'json',
                error: function (err) {
                    console.error("Failed to submit:" + err);
                },
                success: function (response) {
                    console.debug("got response:" + response.msg);
                    showDialog("Pull", response.status, response.msg);
                    if (response.status === "success") {
                        refreshHeadBranchCommits();
                    }
                }
            });
        };

        $("#push-btn")[0].onclick = function () {
            $.ajax({
                url: PUSH_URL,
                timeout: 2000,
                // data: {"selectedBranch": selectedBranch},
                dataType: 'json',
                error: function (err) {
                    console.error("Failed to submit:" + err);
                },
                success: function (response) {
                    console.debug("got response:" + response.msg);
                    showDialog("Push", response.status, response.msg);
                    if (response.status === "success") {
                        refreshHeadBranchCommits();
                    }
                }
            });
        };

        var commitform = document.forms.namedItem("commit_form");
        commitform.addEventListener("submit", function (ev) {
            var message = $("#commit_message")[0].value;
            $.ajax({
                url: COMMIT_URL,
                timeout: 2000,
                method: "POST",
                dataType: 'json',
                data: {"commitmessage": message},
                error: function (err) {
                    console.error("Failed to submit:" + err);
                },
                success: function (response) {
                    console.debug("got response:" + response.msg);
                    showDialog("Commit", response.status, response.msg);
                    if (response.status === 'success') {
                        $("#commit_message")[0].value = "";
                        refreshHeadBranchCommits();
                    }
                }
            });
            ev.preventDefault();
        }, false);

        var branchform = document.forms.namedItem("branch_form");
        branchform.addEventListener("submit", function (ev) {
            var branchname = $("#branch_name")[0].value;
            $.ajax({
                url: NEW_BRANCH_URL,
                timeout: 2000,
                method: "POST",
                dataType: 'json',
                data: {"branchname": branchname},
                error: function (err) {
                    console.error("Failed to submit:" + err);
                },
                success: function (response) {
                    console.debug("got response:" + response.msg);
                    showDialog("Branch", response.status, response.msg);
                    if (response.status === 'success') {
                        $("#branch_name")[0].value = "";
                    }
                }
            });
            ev.preventDefault();
        }, false);

        var rejectform = document.forms.namedItem("reject_form");
        rejectform.addEventListener("submit", function (ev) {
            var rejectreason = $("#reject_reason")[0].value;
            $.ajax({
                url: REJECT_URL,
                timeout: 2000,
                method: "POST",
                dataType: 'json',
                data: {"reason": rejectreason, "id": selectedPr},
                error: function (err) {
                    console.error("Failed to submit:" + err);
                },
                success: function (response) {
                    console.debug("got response:" + response.msg);
                    showDialog("Reject", response.status, response.msg);
                    if (response.status === 'success') {
                        $("#reject_reason")[0].value = "";
                        selectedPr = -1;
                    }
                }
            });
            ev.preventDefault();
        }, false);

        $("#approve-btn")[0].onclick = function () {
            $.ajax({
                url: ACCEPT_URL,
                timeout: 2000,
                data: {"id": selectedPr},
                dataType: 'json',
                error: function (err) {
                    console.error("Failed to submit:" + err);
                },
                success: function (response) {
                    console.debug("got response:" + response.msg);
                    showDialog("Approve", response.status, response.msg);
                    if (response.status === "success") {
                        selectedPr = -1;
                    }
                }
            });
        };

        $("#pr_form").bootstrapValidator({
            // To use feedback icons, ensure that you use Bootstrap v3.1.0 or later
            feedbackIcons: {
                valid: 'glyphicon glyphicon-ok',
                invalid: 'glyphicon glyphicon-remove',
                validating: 'glyphicon glyphicon-refresh'
            },
            fields: {
                pr_message: {
                    row: '.col-xs-10',
                    validators: {
                        notEmpty: {
                            message: 'Mandatory field, please type a PR message'
                        }
                    }
                },
                target_branch: {
                    row: '.col-xs-10',
                    validators: {
                        notEmpty: {
                            message: 'Mandatory field, please select a branch you want to be pulled'
                        }
                    }
                },
                base_branch: {
                    row: '.col-xs-10',
                    validators: {
                        notEmpty: {
                            message: 'Mandatory field, please select a branch you want to pull from'
                        }
                    }
                }
            }
        });

        var prform = document.forms.namedItem("pr_form");
        prform.addEventListener("submit", function (ev) {
            $('#pr_form').data('bootstrapValidator').resetForm();

            // Prevent form submission
            ev.preventDefault();

            var targetbranch = $("#target_branch")[0].value;
            var basebranch = $("#base_branch")[0].value;
            var prmessage = $("#pr_message")[0].value;

            $.ajax({
                url: CREATE_PR_URL,
                timeout: 2000,
                dataType: 'json',
                data: {"targetbranch": targetbranch, "basebranch": basebranch, "prmessage": prmessage},
                error: function () {
                    console.error("Failed to submit");
                },
                success: function (notifications) {
                    appendToNotificationArea(notifications);
                }
            });
        });

        $("#view-btn")[0].onclick = function () {
            $.ajax({
                url: PR_PAGE_URL,
                timeout: 2000,
                data: {"id": selectedPr},
                dataType: 'json',
                error: function (err) {
                    console.error("Failed to submit:" + err);
                },
                success: function (response) {
                    console.debug("got response:" + response.msg);
                    if (response.status === "success") {
                        window.location.href = "pullrequestpage.html";
                    } else {
                        showDialog("View pull request", response.status, response.msg);
                    }
                }
            });
        }
    }
);