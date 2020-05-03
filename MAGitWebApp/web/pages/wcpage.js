var WC_INFO_URL = buildUrlWithContextPath("wc");
var WC_FILE_CONTENT_URL = buildUrlWithContextPath("wcfilecontent");
var COMMIT_URL = buildUrlWithContextPath("commit");
var SAVE_FILE_CONTENT_URL = buildUrlWithContextPath("savefilecontent");
var DELETE_FILE_URL = buildUrlWithContextPath("deletefile");
var CREATE_FILE_URL = buildUrlWithContextPath("createfile");

var selectedFile = null;

function showDialog(title, status, msg) {
    var dialog = $("#dialog");
    $("#dialog-header").text(title);
    $("#dialog-body")[0].innerHTML = '<div class="alert alert-' + status + '" role="alert">' + msg + '</div>';
    dialog.modal('show');
}

function escape_id(id) {
    return id.toString().replace(/ /g, '_').replace(/\\/g, '_').replace(/\./g, '_');
}

function loadFileContent(fileinfo) {
    var area = $("#file_content");
    area[0].value = "";
    var edit = $("#edit-btn");
    if (fileinfo.isDirectory) {
        edit[0].disabled = true;
    } else {
        edit[0].disabled = false;
    }
    area[0].value = fileinfo.content;
}

function refreshFileContent() {
    $.ajax({
        url: WC_FILE_CONTENT_URL,
        timeout: 2000,
        dataType: 'json',
        data: {"file": selectedFile},
        error: function() {
            console.error("Failed to submit");
        },
        success: function(fileinfo) {
            loadFileContent(fileinfo);
        }
    });
}

function populateWCStatusTable(fileslists) {
    var table = $("#deleted_table");
    table[0].tBodies[0].innerText = "";
    fileslists.deleted.forEach(function(file) {
        var filename = escape_id(file);
        table.append('<tr id="fileChange_' + filename + '"></tr>');
        var row = $('#fileChange_' + filename);
        row.html("<td>" + file + "</td>");
    });


    var table = $("#updated_table");
    table[0].tBodies[0].innerText = "";
    fileslists.updated.forEach(function(file) {
        var filename = escape_id(file);
        table.append('<tr id="fileChange_' + filename + '"></tr>');
        var row = $('#fileChange_' + filename);
        row.html("<td>" + file + "</td>");
    });


    var table = $("#created_table");
    table[0].tBodies[0].innerText = "";
    fileslists.created.forEach(function(file) {
        var filename = escape_id(file);
        table.append('<tr id="fileChange_' + filename + '"></tr>');
        var row = $('#fileChange_' + filename);
        row.html("<td>" + file + "</td>");
    });
}

function populateFilesTable(filesList) {
    var table = $("#files_table");
    table[0].tBodies[0].innerText = "";
    filesList.forEach(function(file) {
        var filename = escape_id(file);
        table.append('<tr id="file_' + filename + '"></tr>');
        var row = $('#file_' + filename);
        row.html("<td>" + file + "</td>");
        row[0].classList = ["file-row" + (file === selectedFile ? "-selected" : "")];
        row[0].onclick = function() {
            var selectedRow;
            if (selectedFile !== null) {
                var selectedfilename = escape_id(selectedFile);
                selectedRow =  $('#file_' + selectedfilename);
                var cls = selectedRow[0].classList[0];
                selectedRow[0].classList = [cls.substr(0, (cls.length - "-selected".length))];
            }
            selectedFile = file;
            row[0].classList = ["file-row-selected"];
            $("#save-btn")[0].disabled = true;
            refreshFileContent();
        };
    })
}

function refreshWCPage() {
    var area = $("#file_content");
    area[0].value = "";
    $.ajax({
        url: WC_INFO_URL,
        timeout: 2000,
        //data: "",
        dataType: 'json',
        error: function(err) {
            console.error("Failed to submit: " + err);
        },
        success: function(response) {
            console.debug("got user:" + response.myName);
            $("#username").text(response.myName);
            populateFilesTable(response.wcFiles);
            populateWCStatusTable(response.wcStatus);
            $("#file_path").attr('placeholder', "e.g. \\" + response.activeRepoName + "\\somefile.txt");
        }
    });
}

$(function() { // onload...do
    refreshWCPage();
    $("#edit-btn")[0].disabled = true;
    $("#save-btn")[0].disabled = true;

    $("#edit-btn")[0].onclick = function () {
        document.getElementById("file_content").readOnly = false;
        $("#save-btn")[0].disabled = false;
    };

    $("#save-btn")[0].onclick = function () {
        document.getElementById("file_content").readOnly = true;
        $("#save-btn")[0].disabled = true;
        var fileContent = $("#file_content")[0].value;
        $.ajax({
            url: SAVE_FILE_CONTENT_URL,
            timeout: 2000,
            data: {"filecontent": fileContent, "filename" : selectedFile},
            dataType: 'json',
            error: function (err) {
                console.error("Failed to submit:" + err);
            },
            success: function (response) {
                console.debug("got response:" + response.msg);
                showDialog("Save file", response.status, response.msg);
                if (response.status === "success") {
                    populateWCStatusTable(response.wcStatus);
                }
            }
        });
    };

    $("#delete-btn")[0].onclick = function () {
        $.ajax({
            url: DELETE_FILE_URL,
            timeout: 2000,
            data: {"filename" : selectedFile},
            dataType: 'json',
            error: function (err) {
                console.error("Failed to submit:" + err);
            },
            success: function (response) {
                console.debug("got response:" + response.msg);
                showDialog("Delete file", response.status, response.msg);
                if (response.status === "success") {
                    selectedFile = null;
                    refreshWCPage();
                }
            }
        });
    };


    var createfileform = document.forms.namedItem("create_file");
    createfileform.addEventListener("submit", function (ev) {
        var fileRelativePath = $("#file_path")[0].value;
        $.ajax({
            url: CREATE_FILE_URL,
            timeout: 2000,
            method: "POST",
            dataType: 'json',
            data: {"filerelativepath": fileRelativePath},
            error: function (err) {
                console.error("Failed to submit:" + err);
            },
            success: function (response) {
                console.debug("got response:" + response.msg);
                showDialog("Create file", response.status, response.msg);
                if (response.status === 'success') {
                    $("#file_path")[0].value = "";
                    refreshWCPage();
                }
            }
        });
        ev.preventDefault();
    }, false);


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
                    refreshWCPage();
                }
            }
        });
        ev.preventDefault();
    }, false);
    }
);