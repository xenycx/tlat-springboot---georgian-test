document.addEventListener('DOMContentLoaded', () => {
    const lectureSelect = document.getElementById('lectureSelect');
    const groupSelect = document.getElementById('groupSelect');
    const loadMatrixBtn = document.getElementById('loadMatrixBtn');
    const matrixContainer = document.getElementById('matrixContainer');
    const matrixHead = document.getElementById('matrixHead');
    const matrixBody = document.getElementById('matrixBody');
    const matrixInfo = document.getElementById('matrixInfo');
    
    // Selectors logic
    lectureSelect.addEventListener('change', () => {
        if (lectureSelect.value) {
            groupSelect.disabled = false;
            // Automatically select "ყველა ჯგუფი" when lecture is selected
            groupSelect.value = "0";
            loadMatrixBtn.disabled = false;
        } else {
            groupSelect.disabled = true;
            loadMatrixBtn.disabled = true;
            groupSelect.value = "";
        }
    });

    groupSelect.addEventListener('change', () => {
        if (groupSelect.value) {
            loadMatrixBtn.disabled = false;
        }
    });

    loadMatrixBtn.addEventListener('click', () => {
        loadMatrixData();
    });

    let currentLectureId = null;
    let currentGroupId = null;

    // Load data from API
    async function loadMatrixData() {
        const lectureId = lectureSelect.value;
        const groupId = groupSelect.value;
        
        if (!lectureId || !groupId) return;
        
        currentLectureId = lectureId;
        currentGroupId = groupId;
        
        try {
            // Disable button
            loadMatrixBtn.disabled = true;
            loadMatrixBtn.innerHTML = '<span class="spinner-border spinner-border-sm"></span> იტვირთება...';
            
            // Build URL: omit groupId if it's 0 (All Groups)
            let url = `/api/grading/matrix?lectureId=${lectureId}`;
            if (groupId !== "0") {
                url += `&groupId=${groupId}`;
            }

            const response = await fetch(url);
            if (!response.ok) throw new Error('Data fetch failed');
            const data = await response.json();
            
            if (data.success === false) {
                throw new Error(data.message || 'მონაცემების ჩატვირთვა ვერ მოხერხდა');
            }
            
            renderMatrix(data);
            matrixContainer.classList.remove('d-none');
            
        } catch (error) {
            console.error('Error loading matrix:', error);
            showToast(error.message && error.message !== 'Data fetch failed' ? error.message : 'შეცდომა მონაცემების ჩატვირთვისას', 'bg-danger');
        } finally {
            loadMatrixBtn.disabled = false;
            loadMatrixBtn.innerHTML = '<i class="bi bi-table"></i> ჩატვირთვა';
        }
    }

    // Render logic
    function renderMatrix(data) {
        matrixInfo.textContent = `კვირების რაოდენობა: ${data.semesterWeeks} | დაწყება: ${data.semesterStartDate}`;
        
        // 1. Render Headers
        let headHtml = '<tr><th>სტუდენტი</th><th>ჯგუფი</th>';
        data.columns.forEach((col, i) => {
            headHtml += `<th title="${col.weekStart} - ${col.weekEnd} (მაქს. ${data.weeklyMaxScores[i]} ქულა)">${col.label}</th>`;
        });
        headHtml += '<th>დასწრების ჯამი</th><th>შუალედური</th><th>ფინალური</th><th>სრული ჯამი</th></tr>';
        matrixHead.innerHTML = headHtml;

        // Populate table filter groups
        const uniqueGroups = [...new Set(data.rows.map(r => r.studentGroupName))].sort();
        const tableGroupSelect = document.getElementById('tableGroupSelect');
        tableGroupSelect.innerHTML = '<option value="" selected>ყველა ჯგუფი</option>';
        uniqueGroups.forEach(g => {
            tableGroupSelect.innerHTML += `<option value="${g}">${g}</option>`;
        });

        // 2. Render Rows
        let bodyHtml = '';
        data.rows.forEach(row => {
            bodyHtml += `<tr class="matrix-row" data-student-name="${row.studentName.toLowerCase()}" data-group-name="${row.studentGroupName}">
                <td class="text-start fw-bold">
                    ${row.studentName}
                </td>
                <td class="text-muted small">
                    ${row.studentGroupName}
                </td>`;
            
            data.columns.forEach((col, i) => {
                const cell = row.cells ? row.cells[col.week] : null;
                const maxScore = data.weeklyMaxScores[i];
                if (cell) {
                    // Render existing score cell
                    bodyHtml += `<td class="grade-cell grade-${cell.letterGrade}" 
                                     data-student-id="${row.studentId}"
                                     data-schedule-id="${cell.lectureScheduleId}"
                                     data-score="${cell.score}">
                                    <input type="number" 
                                           class="score-input"
                                           value="${cell.score}" 
                                           title="ქულა: ${cell.score} (მაქს. ${maxScore})"
                                           min="0" max="${maxScore}"
                                           onblur="updateScore(this)">
                                 </td>`;
                } else {
                    // Render empty cell
                    bodyHtml += `<td class="grade-cell grade-empty" 
                                     data-student-id="${row.studentId}"
                                     data-week="${col.week}"
                                     onclick="openAddModal('${row.studentName}', ${row.studentId}, ${col.week}, '${col.label}', ${maxScore})">
                                    —
                                 </td>`;
                }
            });

            // Additional Columns
            const mScore = row.midtermScore !== null ? row.midtermScore : '';
            const fScore = row.finalScore !== null ? row.finalScore : '';
            
            bodyHtml += `
                <td class="fw-bold" id="total-attendance-${row.studentId}">${row.totalAttendanceScore}</td>
                <td class="grade-cell grade-empty" data-student-id="${row.studentId}" data-score="${mScore}">
                    <input type="number" class="score-input" value="${mScore}" placeholder="—" title="მაქს. ${data.maxMidtermScore}" max="${data.maxMidtermScore}" onblur="updateExamScore(this, 'MIDTERM')">
                </td>
                <td class="grade-cell grade-empty" data-student-id="${row.studentId}" data-score="${fScore}">
                    <input type="number" class="score-input" value="${fScore}" placeholder="—" title="მაქს. ${data.maxFinalScore}" max="${data.maxFinalScore}" onblur="updateExamScore(this, 'FINAL')">
                </td>
                <td class="fw-bold" id="total-${row.studentId}">
                    ${row.totalScore} (<span class="text-${getGradeColor(row.totalGrade)}">${row.totalGrade}</span>)
                </td></tr>`;
        });
        
        if (data.rows.length === 0) {
            bodyHtml = `<tr><td colspan="${data.columns.length + 6}" class="text-center py-4">სტუდენტები ვერ მოიძებნა</td></tr>`;
        }
        
        matrixBody.innerHTML = bodyHtml;
        
        // Add events for filters after rendering
        const searchInput = document.getElementById('tableSearchInput');
        const groupSelect = document.getElementById('tableGroupSelect');
        if (searchInput) searchInput.addEventListener('input', applyTableFilters);
        if (groupSelect) groupSelect.addEventListener('change', applyTableFilters);
    }

    // Filter logic
    function applyTableFilters() {
        const searchInput = document.getElementById('tableSearchInput');
        const groupSelect = document.getElementById('tableGroupSelect');
        if (!searchInput || !groupSelect) return;

        const term = searchInput.value.toLowerCase().trim();
        const selectedGroup = groupSelect.value;
        
        const rows = document.querySelectorAll('.matrix-row');
        
        rows.forEach(row => {
            const name = row.dataset.studentName || '';
            const grp = row.dataset.groupName || '';
            
            const matchName = !term || name.includes(term);
            const matchGroup = !selectedGroup || grp === selectedGroup;
            
            if (matchName && matchGroup) {
                row.style.display = '';
            } else {
                row.style.display = 'none';
            }
        });
    }

    // Modal Logic
    const addModal = new bootstrap.Modal(document.getElementById('addRecordModal'));
    const modalForm = document.getElementById('addRecordForm');
    
    window.openAddModal = function(studentName, studentId, week, weekLabel, maxScore) {
        document.getElementById('modalStudentName').textContent = studentName;
        document.getElementById('modalWeekLabel').textContent = weekLabel;
        document.getElementById('modalStudentId').value = studentId;
        document.getElementById('modalWeek').value = week;
        const scoreInput = document.getElementById('modalScore');
        scoreInput.value = '';
        scoreInput.max = maxScore;
        scoreInput.setAttribute('placeholder', `მაქს. ${maxScore}`);
        addModal.show();
    };

    // Helper to get CSRF token
    function getCsrfToken() {
        const name = 'XSRF-TOKEN=';
        const decodedCookie = decodeURIComponent(document.cookie);
        const ca = decodedCookie.split(';');
        for(let i = 0; i <ca.length; i++) {
            let c = ca[i];
            while (c.charAt(0) == ' ') {
                c = c.substring(1);
            }
            if (c.indexOf(name) == 0) {
                return c.substring(name.length, c.length);
            }
        }
        return "";
    }

    modalForm.addEventListener('submit', async (e) => {
        e.preventDefault();
        const btn = modalForm.querySelector('button[type="submit"]');
        btn.disabled = true;
        
        try {
            const payload = {
                studentId: document.getElementById('modalStudentId').value,
                lectureId: currentLectureId,
                groupId: currentGroupId,
                week: document.getElementById('modalWeek').value,
                score: document.getElementById('modalScore').value
            };
            
            const response = await fetch('/api/grading/matrix/create-record', {
                method: 'POST',
                headers: { 
                    'Content-Type': 'application/json',
                    'X-XSRF-TOKEN': getCsrfToken()
                },
                body: JSON.stringify(payload)
            });
            
            if (!response.ok) throw new Error('Update failed');
            const data = await response.json();
            
            if (data.success) {
                showToast('შეფასება დამატებულია', 'bg-success');
                addModal.hide();
                loadMatrixData(); // Reload matrix to reflect new cell
            } else {
                throw new Error(data.message || 'Unknown error');
            }
            
        } catch (error) {
            console.error(error);
            showToast('შეცდომა: ' + error.message, 'bg-danger');
        } finally {
            btn.disabled = false;
        }
    });

    // Update existing score inline
    window.updateScore = async function(inputElem) {
        const td = inputElem.parentElement;
        const studentId = td.dataset.studentId;
        const scheduleId = td.dataset.scheduleId;
        const oldScore = td.dataset.score;
        const newScore = inputElem.value;
        
        if (oldScore === newScore) return; // No change
        if (newScore === '') {
            inputElem.value = oldScore; // Revert
            return;
        }
        
        const maxScore = Number(inputElem.max);
        if (Number(newScore) > maxScore) {
            showToast(`მაქსიმალური ქულა არის ${maxScore}`, 'bg-danger');
            inputElem.value = oldScore;
            return;
        }

        try {
            inputElem.disabled = true;
            
            const payload = {
                studentId: studentId,
                lectureScheduleId: scheduleId,
                score: newScore
            };
            
            const response = await fetch('/api/grading/matrix/update-score', {
                method: 'POST',
                headers: { 
                    'Content-Type': 'application/json',
                    'X-XSRF-TOKEN': getCsrfToken()
                },
                body: JSON.stringify(payload)
            });
            
            if (!response.ok) throw new Error('Update failed');
            const data = await response.json();
            
            if (data.success) {
                // Update cell
                td.dataset.score = newScore;
                td.className = `grade-cell grade-${data.letterGrade}`;
                inputElem.title = `ქულა: ${newScore}`;
                
                // Update total
                const totalTd = document.getElementById(`total-${studentId}`);
                const attendanceTotalTd = document.getElementById(`total-attendance-${studentId}`);
                
                if (totalTd) {
                    totalTd.innerHTML = `${data.newTotal} (<span class="text-${getGradeColor(data.newTotalGrade)}">${data.newTotalGrade}</span>)`;
                }
                if (attendanceTotalTd) {
                    attendanceTotalTd.textContent = data.newTotalAttendance;
                }
                
                showToast('შეფასება განახლებულია', 'bg-success');
            }
            
        } catch (error) {
            console.error(error);
            inputElem.value = oldScore; // Revert on error
            showToast('შეცდომა შენახვისას', 'bg-danger');
        } finally {
            inputElem.disabled = false;
        }
    };

    // Update exam score (Midterm/Final)
    window.updateExamScore = async function(inputElem, examType) {
        const td = inputElem.parentElement;
        const studentId = td.dataset.studentId;
        const oldScore = td.dataset.score;
        const newScore = inputElem.value;
        
        if (oldScore === newScore) return; 
        
        const maxScore = Number(inputElem.max);
        if (newScore !== '' && Number(newScore) > maxScore) {
            showToast(`მაქსიმალური ქულა არის ${maxScore}`, 'bg-danger');
            inputElem.value = oldScore;
            return;
        }

        try {
            inputElem.disabled = true;
            
            const payload = {
                studentId: studentId,
                lectureId: currentLectureId,
                groupId: currentGroupId,
                examType: examType,
                score: newScore
            };
            
            const response = await fetch('/api/grading/matrix/update-exam', {
                method: 'POST',
                headers: { 
                    'Content-Type': 'application/json',
                    'X-XSRF-TOKEN': getCsrfToken()
                },
                body: JSON.stringify(payload)
            });
            
            if (!response.ok) throw new Error('Update failed');
            const data = await response.json();
            
            if (data.success) {
                td.dataset.score = newScore;
                
                const totalTd = document.getElementById(`total-${studentId}`);
                if (totalTd) {
                    totalTd.innerHTML = `${data.newTotal} (<span class="text-${getGradeColor(data.newTotalGrade)}">${data.newTotalGrade}</span>)`;
                }
                
                const attendanceTotalTd = document.getElementById(`total-attendance-${studentId}`);
                if (attendanceTotalTd) {
                    attendanceTotalTd.textContent = data.newTotalAttendance;
                }
                
                showToast('გამოცდის ქულა განახლებულია', 'bg-success');
            } else {
                throw new Error(data.message);
            }
            
        } catch (error) {
            console.error(error);
            inputElem.value = oldScore;
            showToast(error.message && error.message !== 'Update failed' ? error.message : 'შეცდომა შენახვისას', 'bg-danger');
        } finally {
            inputElem.disabled = false;
        }
    };

    function getGradeColor(grade) {
        const colors = {
            'A': 'success',
            'B': 'primary',
            'C': 'warning',
            'D': 'orange',
            'E': 'danger',
            'F': 'danger'
        };
        return colors[grade] || 'secondary';
    }

    function showToast(message, colorClass) {
        const toastEl = document.getElementById('toastMessage');
        const toastBody = document.getElementById('toastBody');
        
        toastEl.className = `toast align-items-center text-white border-0 ${colorClass}`;
        toastBody.textContent = message;
        
        const toast = new bootstrap.Toast(toastEl);
        toast.show();
    }
});