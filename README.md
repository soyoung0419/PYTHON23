## <로그인 및 접근 권한 시퀀스 다이어그램 설명>

###### *로 표시된 메서드는 클래스 다이어그램에 포함되어 있지 않으며, 시퀀스 다이어그램 작성 과정에서 설명 목적상 추가적으로 정의된 메서드이다. <br>

### 1.**접속 및 커뮤니티 메뉴 선택** <br>
- 1-1. 사용자는 웹 브라우저를 통해 홈페이지에 접속하고, 상단 메뉴에서 커뮤니티 게시판 항목을 선택한다.<br>
  이 요청은 enterHomepage()*,  selectCommunityMenu()* 메서드를 통해 처리된다.

### 2. **회원 여부 확인 절차**<br>
- 2-1. 커뮤니티 페이지 진입 시, 시스템은 checkMembership()* 메서드를 호출하여 사용자의 회원 여부를 확인한다.
- 2-2. 내부적으로 existsById(userId)를 호출하여 데이터베이스에서 주어진 UserId를 가진 User의 존재 여부를 확인한다.

### 3. **로그인 절차**<br>

&nbsp;&nbsp;&nbsp;[비회원인 경우] <br>
- 3-1. 사용자가 등록된 회원이 아닌 경우 isNotMember(), 시스템은 goToSignInPage()* 메소드를 호출하여 회원가입 페이지로 이동시킨다.
- 3-2. 사용자는 회원가입 요청을 보낸다. → requestSignIn()
- 3-3. verifyRealName(userId, authInfo) 메서드를 호출하여 사용자의 실명인증 절차를 진행한다.
- 3-4. 인증에 성공하면, signInUser(userInfo)를 통해 사용자 계정이 생성된다. 
- 3-5. login(userId, password) 메서드를 호출하여 로그인 절차를 진행한다. 
- 3-6. 실명인증에 실패한 경우, retryRequestSignIn()*로 실명인증 재시도가 유도된다.

&nbsp;&nbsp;&nbsp;[기존 회원인 경우] <br>
- 3-7. 기존 회원인 경우 login(userId, password)로 로그인 요청이 전송되고 다음 단계로 진행한다.

### 4.  **권한 확인 및 접근 제어 절차**<br>
- 4-1. 로그인 후, 시스템은 해당 사용자의 권한을 조회한다. 먼저, hasAdminAccess()로 관리자 여부를 확인한다.
- 4-2. getPermissions()*으로 권한 조회를 진행한다.
- 4-3. getType() 메소드로 사용자의 권한 유형을 결정한다.

&nbsp;&nbsp;&nbsp;[학생 권한일 경우] <br>
- 4-4. checkAccess()*를 통해 커뮤니티 게시판 접근이 허용된다.
- 4-5. manageContent()*를 통해 글 작성, 댓글 작성/수정/삭제 등이 가능해진다.

&nbsp;&nbsp;&nbsp;[관리자 권한일 경우] <br>
- 4-6. allowAdminAccess()*로 관리자 권한이 부여된다.
- 4-7. useAdminFunctions()*를 통해 게시판 관리, 공지 등록 등의 기능의 사용이 가능해진다.

### 5. **로그아웃 절차**<br>
- 5-1. 사용자는 활동 종료 시 로그아웃을 수행한다. → logout()*
