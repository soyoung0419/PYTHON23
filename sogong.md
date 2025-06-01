## 게시글 작성 시퀀스 다이어그램


1️⃣**게시판 목록 불러오기** 
- 1-1. 사용자가 웹 브라우저를 통해 홈페이지에 접속한다. enterHomepage()*
- 1-2. 커뮤니티 메뉴를 선택한다. selectCommunityMenu()
- 1-3. WebBrowser는 게시판 접근을 위해 requestBoardAccess()를 CommunityService에 요청한다. 
1-4. 게시판을 선택한다. selectBoard()
1-5. 게시글 목록을 받아오기 위해 AuthService를 통해 getPostList()를 호출한다. 가장 최근에 작성된 Post를 우선하여 순차적으로 반환한다.


2. 게시글 읽기 
2-1. 사용자가 특정 게시글을 선택하면, WebBrowser는 selectPost()를 실행한다.

2-2. CommunityService는 해당 게시글의 다양한 세부 정보를 DB에서 순차적으로 조회한다

2-3.  게시글 기본 정보 조회
getTitle()로 게시글 제목을 조회한다. 
getDate()로 작성 날짜를 조회한다.
getContent()로 본문 내용을 조회한다. 

2-4. 첨부파일 여부 확인
isFile()로 첨부파일 여부를 판단한다.

2-5. 첨부파일 정보를 조회한다.
isfile()을 통해 첨부파일 여부 확인 후 getFileList()로 파일 인덱스 리스트 반환한다.파일 인덱스 리스트 반환한다.
getName()으로 파일 이름을 조회한다. 
getSize()으로 파일 용량을 조회한다. 
getViewerIndex()로 파일 형식에 적합한 파일 뷰어 인덱스를 가져온다. 


2-6. 댓글 목록을 조회한다. 
getCommentList()으로 해당 게시글의 모든 댓글 리스트를 반환한다. 




3. 게시글 작성
3-1. 게시글 작성 기능을 실행하면, createPost() 메서드를 호출하여 커뮤니티 서비스에 게시글 작성을 요청한다. 

3-2. addIndex()로 게시글 고유 인덱스를 자동 생성한다. 

3-3. addDate()로 현재 시간 기준으로 작성일자를 자동 저장한다. 
3-4. addTitle(), addContent()로 사용자가 입력한 제목과 본문을 저장한다.
3-5. 첨부파일 정보 저장
addFileIndex()로 첨부파일에 대해 새로운 파일 인덱스를 생성 후 저장한다. 
addFileTitle()로  파일의 이름(제목)을 DB에 저장한다. 

[파일 업로드 처리]
3-3.
파일을 첨부하면 WebBrowser는 uploadFile()을 호출하고,
CommunityService는 FileDB에 다음 정보를 저장한다:

addSize(): 파일의 사이즈
addType(): 파일의 형식 
addUploadDate(): 파일의 업로드 시간을 현재로 설정

setViewerIndex(): 파일 형식에 따른 뷰어 인덱스 등록

appendFileList(): 파일리스트에 첨부파일 추가 


4. 댓글 작성 및 삭제

[댓글 작성 시]
4-1. 사용자가 댓글 기능을 실행하면,  createComment()를 호출한다. 
4-2. appendCommentList() 메소드를 통해 댓글 내용, 작성자 ID, 비밀번호를 댓글 리스트에 추가한다.

[댓글 삭제 시]
4-3. 댓글 삭제 시 deleteComment()가 호출된다.
4-4. delComment(CommentID, CommentPassword)를 통해 인증 후 댓글이 삭제된다. 



5.  공지사항 설정
5-1. 사용자가 공지사항 등록을 시도한다. SetNoticeButton()
6-2. getAdminType()으로 사용자의 권한을 조회한다.

[학생권한인 경우]
6-3. 학생 권한의 경우, 공지사항 등록 권한이 없기 때문에 시스템은 AccessDenied() 메서드를 호출하여 접근이 거부되었음을 알린다.
6-4. setNoticeFailed()를 호출해 사용자에게 접근 거부를 알린다. 


[관리자 권한인 경우]
6-5. 사용자가 관리자 권한을 가진 경우, AccessGranted() 메서드를 통해 접근이 허용되었음을 나타낸다.
6-6. setNoticeSuccess()를 호출해 관리자에게 공지사항 접근 허용을 알린다. 
