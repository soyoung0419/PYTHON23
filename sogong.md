## <게시글 작성 시퀀스 다이어그램 설명>

###### *로 표시된 메서드는 클래스 다이어그램에 포함되어 있지 않으며, 시퀀스 다이어그램 작성 과정에서 설명 목적상 추가적으로 정의된 메서드이다. <br>

### 1. **게시판 목록 불러오기** <br>
• 1-1. 사용자가 웹 브라우저를 통해 홈페이지에 접속한다. → enterHomepage()*  
• 1-2. 커뮤니티 메뉴를 선택한다. → selectCommunityMenu()*  
• 1-3. requestBoardAccess()를 통해 게시판 접근을 요청한다.  
• 1-4. 게시판을 선택한다. → selectBoard()  
• 1-5. 게시글 목록을 받아오기 위해 getPostList()를 호출한다. 가장 최근에 작성된 Post를 우선하여 순차적으로 반환한다.

### 2. **게시글 읽기** <br>
• 2-1. 사용자가 특정 게시글을 선택한다. → selectPost()  
• 2-2. 게시글의 세부 정보를 조회한다.  

• 2-3. [게시글 기본 정보 조회]<br> 
&nbsp;&nbsp;&nbsp;• getTitle()로 게시글 제목을 조회한다.  
&nbsp;&nbsp;&nbsp;• getDate()로 게시글 작성 날짜를 조회한다.  
&nbsp;&nbsp;&nbsp;• getContent()로 본문 내용을 조회한다.  

• 2-4. [첨부파일 여부 확인]<br> 
&nbsp;&nbsp;&nbsp;• isFile()로 첨부파일 여부를 판단한다.  

• 2-5. [첨부파일 정보를 조회]<br>  
&nbsp;&nbsp;&nbsp;• getFileList()로 파일 인덱스 리스트를 반환한다.  
&nbsp;&nbsp;&nbsp;• getName()으로 파일 이름을 조회한다.  
&nbsp;&nbsp;&nbsp;• getSize()으로 파일 용량을 조회한다.  
&nbsp;&nbsp;&nbsp;• getViewerIndex()로 파일 형식에 적합한 파일 뷰어 인덱스를 가져온다.  

• 2-6. [댓글 조회]<br>  
&nbsp;&nbsp;&nbsp;• getCommentList()로 해당 게시글의 모든 댓글 리스트를 반환한다.


### 3. **게시글 작성 절차** <br>
• 3-1. 사용자가 게시글 작성 기능을 실행하면, createPost() 메서드를 호출하여 커뮤니티 서비스에 게시글 작성을 요청한다.  
• 3-2. addIndex()로 게시글 고유 인덱스를 자동으로 생성한다.  
• 3-3. addDate() 현재 시간 기준으로 작성 시간을 자동으로 등록한다.  
• 3-4. addTitle(), addContent()로 사용자가 입력한 제목 및 본문을 저장한다.  

• 3-5. [첨부파일 정보 저장] <br>
  • addFileIndex()로 첨부파일 인덱스를 생성한다.  
  • addFileTitle()로 파일의 이름을 DB에 저장한다.

### 4. **파일 업로드 처리** <br>
• 4-1. 사용자가 파일을 업로드하면, WebBrowser가 uploadFile()을 호출한다.  
• 4-2. CommunityService는 File DB에 파일 정보를 저장한다.  
&nbsp;&nbsp;&nbsp;• addSize()로 파일 크기를 저장한다.  
&nbsp;&nbsp;&nbsp;• addType()으로 파일 형식을 저장한다.  
&nbsp;&nbsp;&nbsp;• addUploadDate()로 파일 업로드 시간을 저장한다.  
&nbsp;&nbsp;&nbsp;• setViewerIndex()로 파일 형식 기반 뷰어 인덱스를 설정한다.  
&nbsp;&nbsp;&nbsp;• appendFileList()로 파일리스트에 첨부파일을 추가한다.

### 5. **댓글 작성 및 삭제** <br>

&nbsp;[댓글 작성 시] <br>
• 5-1. 사용자가 댓글 기능을 실행하면, createComment()를 호출한다.  
• 5-2. appendCommentList() 메소드를 통해 댓글 내용, 작성자 ID, 비밀번호를 댓글 리스트에 추가한다.

&nbsp;[댓글 삭제 시] <br>
• 5-3. 댓글 삭제 시 deleteComment()가 호출된다.  
• 5-4. delComment(CommentID, CommentPassword)를 통해 인증 후 댓글이 삭제된다.

### 6. **공지사항 설정 절차** <br>
• 6-1. 사용자가 공지사항 등록 버튼을 누른다. → SetNoticeButton()*  
• 6-2. getAdminType()으로 사용자의 권한을 조회한다.

&nbsp;[학생 권한일 경우] <br>
• 6-3. 공지사항 등록 권한이 없기 때문에 시스템은 AccessDenied() 메서드를 호출하여 접근을 거부한다.  
• 6-4. setNoticeFailed()로 사용자에게 공지 등록 실패를 알린다.

&nbsp;[관리자 권한일 경우] <br>
• 6-5. 사용자가 관리자 권한을 가진 경우, AccessGranted() 메서드를 통해 접근이 허용되었음을 나타낸다.  
• 6-6. setNoticeSuccess()를 호출해 관리자에게 공지사항 접근 허용을 알린다.
