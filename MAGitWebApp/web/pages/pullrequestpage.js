var PR_INFO_URL = buildUrlWithContextPath("prinfo");
var FILES_CHANGES_URL = buildUrlWithContextPath("prchanges");
var FILE_CONTENT_URL = buildUrlWithContextPath("filecontent");

var selectedCommit = null;
var selectedFile = null;

function escape_id(id) {
    return id.toString().replace(/ /g, '_').replace(/\\/g, '_').replace(/\./g, '_');
}

function loadFileContent(content) {
    var area = $("#file_content");
    area[0].value = "";
    area[0].value = content;
}

function refreshFileContent() {
    $.ajax({
        url: FILE_CONTENT_URL,
        timeout: 2000,
        dataType: 'json',
        data: {"file": selectedFile, "commitsha1": selectedCommit},
        error: function() {
            console.error("Failed to submit");
        },
        success: function(content) {
            loadFileContent(content);
        }
    });
}

function populateSelectedCommitFilesTable(fileslists) {
    selectedFile = null;
    var table = $("#deleted_table");
    table[0].tBodies[0].innerText = "";
    fileslists.deleted.forEach(function(file) {
        var filename = escape_id(file);
        table.append('<tr id="file_' + filename + '"></tr>');
        var row = $('#file_' + filename);
        row.html("<td>" + file + "</td>");
    });


    var table = $("#updated_table");
    table[0].tBodies[0].innerText = "";
    fileslists.updated.forEach(function(file) {
        var filename = escape_id(file);
        table.append('<tr id="file_' + filename + '"></tr>');
        var row = $('#file_' + filename);
        row.html("<td>" + file + "</td>");
        row[0].classList = ["file-row" + (file === selectedFile ? "-selected" : "")];
        row[0].onclick = function() {
            var selectedRow;
            if (selectedFile !== null) {
                var selectedFileId = escape_id(selectedFile);
                selectedRow =  $('#file_' + selectedFileId);
                var cls = selectedRow[0].classList[0];
                selectedRow[0].classList = [cls.substr(0, (cls.length - "-selected".length))];
            }
            selectedFile = file;
            row[0].classList = ["file-row-selected"];
            refreshFileContent();
        };
    });


    var table = $("#created_table");
    table[0].tBodies[0].innerText = "";
    fileslists.created.forEach(function(file) {
        var filename = escape_id(file);
        table.append('<tr id="file_' + filename + '"></tr>');
        var row = $('#file_' + filename);
        row.html("<td>" + file + "</td>");
        row[0].classList = ["file-row" + (file === selectedFile ? "-selected" : "")];
        row[0].onclick = function() {
            var selectedRow;
            if (selectedFile !== null) {
                var selectedFileId = escape_id(selectedFile);
                selectedRow =  $('#file_' + selectedFileId);
                var cls = selectedRow[0].classList[0];
                selectedRow[0].classList = [cls.substr(0, (cls.length - "-selected".length))];
            }
            selectedFile = file;
            row[0].classList = ["file-row-selected"];
            refreshFileContent();
        };
    });
}

function refreshFilesChangesTable(selectedPrevSha1) {
    $.ajax({
        url: FILES_CHANGES_URL,
        timeout: 2000,
        dataType: 'json',
        data: {"sha1": selectedCommit, "prevsha1" : selectedPrevSha1},
        error: function() {
            console.error("Failed to submit");
        },
        success: function(fileslists) {
            populateSelectedCommitFilesTable(fileslists);
        }
    });
}

function populateCommitsTable(commits) {
    var table = $("#commits_table");
    commits.forEach(function(commit) {
        table.append('<tr id="commit_' + commit.sha1 + '"></tr>');
        var row = $('#commit_' + commit.sha1);
        row.html("<td class='commit-col1'>" + commit.sha1 + "</td><td class='commit-col2'>" + commit.message + "</td><td class='commit-col3'>" +
            commit.date + "</td><td class='commit-col4'>" + commit.author + "</td>");
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
            refreshFilesChangesTable(commit.prevCommitSha1);
        };
    })
}

function refreshPRPage() {
    $.ajax({
        url: PR_INFO_URL,
        timeout: 2000,
        //data: "",
        error: function(err) {
            console.error("Failed to submit: " + err);
        },
        success: function(response) {
            console.debug("got user:" + response.myUsername);
            $("#username").text(response.myUsername);
            populateCommitsTable(response.commits);
        }
    });
}

$(function() { // onload...do
        refreshPRPage();

    }
);