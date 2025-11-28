import java.util.Scanner;
import java.util.ArrayList;
import java.util.List;
import java.time.LocalDate;

public class CampusPlannerApp {

    // ====== Console App (Main) ======
    public static void main(String[] args) {
        System.out.println("= CampusPlanner Console (통합/Local DB) =");

        // Load persisted store
        LocalDatabaseManager db = new LocalDatabaseManager();
        LocalStore store = db.load();

        // ---- Friend / Diary 필드만 초기화 ----
        if (store.friends == null) store.friends = new ArrayList<>();
        if (store.friendSeq <= 0) store.friendSeq = 1;

        if (store.diaries == null) store.diaries = new ArrayList<>();
        if (store.diarySeq <= 0) store.diarySeq = 1;

        // ---- DAO / VM 생성 (Friend + Diary만) ----
        FriendDao friendDao = new FriendDao(store, db);
        FriendViewModel friendVM = new FriendViewModel(friendDao);

        DiaryDao diaryDao = new DiaryDao(store, db);
        CalendarViewModel calVM = new CalendarViewModel(diaryDao); // TaskDao 제거되었으므로 null

        // ---- Seed (Friend + Diary만) ----
        if (store.friends.isEmpty() && store.diaries.isEmpty()) {
            seed(friendVM, calVM);
            db.save(store);
        }

        try (Scanner sc = new Scanner(System.in)) {
            loop:
            while (true) {
                printMenu();
                String cmd = sc.nextLine().trim();
                try {
                    switch (cmd) {
                        case "3":
                            handleFriends(sc, friendVM);
                            break;
                        case "4":
                            handleDiary(sc, calVM);
                            break;
                        case "0":
                            break loop;
                        default:
                            System.out.println("알 수 없는 명령");
                    }
                } catch (Exception e) {
                    System.out.println("오류: " + e.getMessage());
                }
            }
        }

        // 종료 시 저장
        db.save(store);
        System.out.println("종료 (데이터 저장됨)");
    }

    // ---- 메뉴 출력 (Friend + Diary만) ----
    static void printMenu() {
        System.out.println();
        System.out.println("========================================");
        System.out.println(" [메뉴]");
        System.out.println("========================================");
        System.out.println(" 3: 친구");
        System.out.println(" 4: 다이어리");
        System.out.println(" 0: 종료");
        System.out.println("----------------------------------------");
        System.out.print("> ");
    }

    // ---- Friend 핸들러 (원본 그대로) ----
    static void handleFriends(Scanner sc, FriendViewModel vm) {
        while (true) {
            System.out.println("========================================");
            System.out.println(" [친구]");
            System.out.println("========================================");
            System.out.println(" a : 추가");
            System.out.println(" u : 수정");
            System.out.println(" d : 삭제");
            System.out.println(" l : 목록");
            System.out.println(" t : 태그 수정");
            System.out.println(" ft: 태그 검색");
            System.out.println(" b : 뒤로가기");
            System.out.println("----------------------------------------");
            System.out.print("> ");
            String c = sc.nextLine().trim();
            if (c.equals("b")) return;

            try {
                switch (c) {
                    case "a":
                        while (true) {
                            try {
                                System.out.print("이름: ");
                                String name = sc.nextLine();
                                System.out.print("UID (형식: uid_숫자): ");
                                String uid = sc.nextLine();
                                Friend f = vm.add(name, uid);
                                System.out.println("추가됨: " + f);
                                break;
                            } catch (Exception e) {
                                System.out.println("오류: " + e.getMessage());
                                System.out.println("다시 입력하시겠습니까? (y = 다시, n = 취소)");
                                String again = sc.nextLine().trim();
                                if (!again.equalsIgnoreCase("y")) {
                                    System.out.println("친구 추가 취소됨.");
                                    break;
                                }
                            }
                        }
                        break;

                    case "l":
                        vm.list().forEach(System.out::println);
                        break;

                    case "u":
                        System.out.print("수정할 친구 ID: ");
                        long uidTarget = Long.parseLong(sc.nextLine());
                        System.out.print("새 이름(Enter 시 변경 없음): ");
                        String newName = sc.nextLine();
                        System.out.print("새 UID(uid_숫자, Enter 시 변경 없음): ");
                        String newUid = sc.nextLine();
                        vm.update(uidTarget,
                                newName.isBlank() ? null : newName,
                                newUid.isBlank() ? null : newUid);
                        System.out.println("수정 완료");
                        break;

                    case "d":
                        System.out.print("삭제할 ID: ");
                        long delId = Long.parseLong(sc.nextLine());
                        vm.delete(delId);
                        System.out.println("삭제됨");
                        break;

                    case "t":
                        System.out.print("태그 수정할 ID: ");
                        long tagId = Long.parseLong(sc.nextLine());
                        System.out.print("새 태그 입력: ");
                        String tag = sc.nextLine();
                        vm.updateTag(tagId, tag);
                        System.out.println("태그 수정 완료");
                        break;

                    case "ft":
                        System.out.print("검색할 태그 입력: ");
                        String search = sc.nextLine();
                        List<Friend> filtered = vm.findByTag(search);
                        if (filtered.isEmpty()) System.out.println("해당 태그의 친구가 없습니다.");
                        else filtered.forEach(System.out::println);
                        break;

                    default:
                        System.out.println("? (알 수 없는 명령)");
                }

            } catch (Exception e) {
                System.out.println("오류: " + e.getMessage());
            }
        }
    }

    // ---- Diary 핸들러 (원본 그대로) ----
    static void handleDiary(Scanner sc, CalendarViewModel vm) {
        while (true) {
            System.out.println("========================================");
            System.out.println(" [다이어리]");
            System.out.println("========================================");
            System.out.println(" a : 추가");
            System.out.println(" l : 날짜별 조회");
            System.out.println(" all : 전체 목록");
            System.out.println(" u : 수정");
            System.out.println(" d : 삭제");
            System.out.println(" img : 이미지 열기");
            System.out.println(" imgdel: 이미지 삭제");
            System.out.println(" k : 키워드 검색");
            System.out.println(" b : 뒤로가기");
            System.out.println("----------------------------------------");
            System.out.print("> ");
            String c = sc.nextLine().trim();
            if (c.equals("b")) return;

            switch (c) {
                case "a":
                    System.out.print("날짜(yyyy-MM-dd): ");
                    LocalDate d = DateTimeUtils.parseDate(sc.nextLine());
                    System.out.print("내용: ");
                    String text = sc.nextLine();
                    System.out.print("이미지 경로(선택): ");
                    String img = sc.nextLine();
                    Diary dd = vm.addDiary(d, text, img.isBlank() ? null : img);
                    System.out.println("추가됨: " + dd);
                    break;

                case "all":
                    System.out.println("[전체 일기 목록]");
                    vm.getAllDiaries().forEach(System.out::println);
                    break;

                case "l":
                    System.out.print("날짜(yyyy-MM-dd): ");
                    LocalDate date = DateTimeUtils.parseDate(sc.nextLine());
                    System.out.println("[일기]");
                    vm.diariesOn(date).forEach(System.out::println);
                    System.out.println("[해당 날짜 마감 할일]");
                    // Task 기능 삭제 → 빈 출력만 유지
                    System.out.println("(Task 기능 제거됨)");
                    break;

                case "d":
                    System.out.print("삭제할 일기 ID: ");
                    long delId = Long.parseLong(sc.nextLine());
                    vm.deleteDiary(delId);
                    System.out.println("삭제됨");
                    break;

                case "u":
                    System.out.print("수정할 일기 ID: ");
                    long uid = Long.parseLong(sc.nextLine());
                    System.out.print("새 내용 (Enter: 유지): ");
                    String newText = sc.nextLine();
                    System.out.print("새 이미지 경로 (Enter: 유지): ");
                    String newImg = sc.nextLine();
                    vm.updateDiary(uid, newText, newImg.isBlank() ? null : newImg);
                    System.out.println("수정 완료");
                    break;

                case "img":
                    System.out.print("이미지 볼 일기 ID: ");
                    long imgId = Long.parseLong(sc.nextLine());
                    vm.getAllDiaries()
                            .stream()
                            .filter(di -> di.getId() == imgId)
                            .findFirst()
                            .ifPresentOrElse(
                                    di -> ImageHelper.openImage(di.getImagePath()),
                                    () -> System.out.println("해당 ID의 일기를 찾을 수 없습니다.")
                            );
                    break;

                case "imgdel":
                    System.out.print("이미지 삭제할 일기 ID: ");
                    long delImgId = Long.parseLong(sc.nextLine());
                    vm.getAllDiaries().stream()
                            .filter(di -> di.getId() == delImgId)
                            .findFirst()
                            .ifPresentOrElse(
                                    di -> {
                                        try {
                                            vm.updateDiary(di.getId(), di.getText(), "__DELETE_IMAGE__");
                                            System.out.println("이미지 삭제 완료");
                                        } catch (Exception e) {
                                            System.out.println("오류: " + e.getMessage());
                                        }
                                    },
                                    () -> System.out.println("해당 ID의 일기를 찾을 수 없습니다.")
                            );
                    break;

                case "k":
                    System.out.print("검색할 키워드: ");
                    String keyword = sc.nextLine();
                    List<Diary> found = vm.searchKeyword(keyword);
                    if (found.isEmpty()) System.out.println("해당 키워드 일기 없음.");
                    else found.forEach(System.out::println);
                    break;

                default:
                    System.out.println("?");
            }
        }
    }

    // ---- Friend & Diary Seed ----
    static void seed(FriendViewModel friendVM, CalendarViewModel calVM) {
        friendVM.add("민수", "uid_123");
        friendVM.add("지은", "uid_456");
        calVM.addDiary(LocalDate.now(), "중간고사 대비", null);
    }
}



// getId() , getText() 등 getter로 변경해야 함
