## <게시글 작성 시퀀스 다이어그램 설명>

###### *로 표시된 메서드는 클래스 다이어그램에 포함되어 있지 않으며, 시퀀스 다이어그램 작성 과정에서 설명 목적상 추가적으로 정의된 메서드이다. <br>

1️⃣ **게시판 목록 불러오기** <br>
- 1-1. 사용자가 웹 브라우저를 통해 홈페이지에 접속한다. → `enterHomepage()`*
- 1-2. 커뮤니티 메뉴를 선택한다. → `selectCommunityMenu()`*
- 1-3. `requestBoardAccess()`를 통해 게시판 접근을 요청한다.
- 1-4. 게시판을 선택한다. → `selectBoard()`
- 1-5. 게시글 목록을 받아오기 위해  `getPostList()`를 호출한다. 가장 최근에 작성된 Post를 우선하여 순차적으로 반환한다.


2️⃣ **게시글 읽기** <br>
- 2-1. 사용자가 특정 게시글을 선택한다. → `selectPost()`
- 2-2. 게시글의 세부 정보를 조회한다.

&nbsp;&nbsp;&nbsp;[게시글 기본 정보 조회] <br>
- `getTitle()`로 게시글 제목을 조회한다.
- `getDate()`로 게시글 작성 날짜를 조회한다.
- `getContent()`로 본문 내용을 조회한다.

&nbsp;&nbsp;&nbsp;[첨부파일 여부 확인 및 정보 조회] <br>
- `isFile()` → 첨부파일 존재 여부 판단  
- `getFileList()` → 첨부파일 인덱스 리스트 반환  
- `getName()` → 파일 이름 조회  
- `getSize()` → 파일 용량 조회  
- `getViewerIndex()` → 뷰어 인덱스 조회

&nbsp;&nbsp;&nbsp;[댓글 조회] <br>
- `getCommentList()` → 댓글 목록 반환

3️⃣ **게시글 작성 절차** <br>
- 3-1. 사용자가 게시글 작성을 요청한다. → `createPost()`*
- 3-2. `addIndex()` → 게시글 고유 번호 자동 생성
- 3-3. `addDate()` → 작성 시간 자동 등록
- 3-4. `addTitle()`, `addContent()` → 제목 및 본문 저장

&nbsp;&nbsp;&nbsp;[첨부파일 정보 저장] <br>
- `addFileIndex()` → 첨부파일 인덱스 생성
- `addFileTitle()` → 파일 이름 저장

4️⃣ **파일 업로드 처리** <br>
- 4-1. WebBrowser가 `uploadFile()`을 호출한다.  
- 4-2. CommunityService는 다음과 같은 메서드로 FileDB에 저장한다:
  - `addSize()` → 파일 크기 저장  
  - `addType()` → 파일 형식 저장  
  - `addUploadDate()` → 업로드 시간 저장  
  - `setViewerIndex()` → 파일 형식 기반 뷰어 인덱스 설정  
  - `appendFileList()` → 게시글에 첨부파일 연결

5️⃣ **댓글 작성 및 삭제** <br>

&nbsp;&nbsp;&nbsp;[댓글 작성 시] <br>
- 5-1. 사용자가 댓글을 작성한다. → `createComment()`*
- 5-2. `appendCommentList(Comment, UserID, Password)` → 댓글 리스트에 추가

&nbsp;&nbsp;&nbsp;[댓글 삭제 시] <br>
- 5-3. 사용자가 댓글 삭제 요청을 보낸다. → `deleteComment()`*
- 5-4. `delComment(CommentID, Password)` → 인증 후 댓글 삭제

6️⃣ **공지사항 설정 절차** <br>
- 6-1. 사용자가 공지사항 등록 버튼을 누른다. → `SetNoticeButton()`*
- 6-2. `getAdminType()`으로 사용자의 권한을 조회한다.

&nbsp;&nbsp;&nbsp;[학생 권한일 경우] <br>
- 6-3. `AccessDenied()` → 접근 거부 처리  
- 6-4. `setNoticeFailed()` → 공지 등록 실패 안내

&nbsp;&nbsp;&nbsp;[관리자 권한일 경우] <br>
- 6-5. `AccessGranted()` → 접근 허용 처리  
- 6-6. `setNoticeSuccess()` → 공지 등록 성공 안내
