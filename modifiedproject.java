// File: CampusPlannerConsole.java
// Console-only backend version of CampusPlanner (통합본)

import java.io.*;
import java.nio.file.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.*;

public class CampusPlannerConsole {

    // ====== Utils ======



    // author: soyoung0419, expected: 친구 정보 보관(이름/UID) , developed: 단순 식별자·문자열 출력
    static class Friend implements Serializable {
        private static final long serialVersionUID = 1L;
        final long id;
        String name;
        String uid; // 외부 참조용 문자열
        String tag;
        Friend(long id, String name, String uid) { this.id = id; this.name = name; this.uid = uid; this.tag = "null"; }
        @Override public String toString() { return String.format("[#%d] %s (uid=%s, tag=%s)", id, name, uid, tag); }
        @Override public boolean equals(Object o){ if(this==o) return true; if(!(o instanceof Friend)) return false; return id==((Friend)o).id; }
        @Override public int hashCode(){ return Long.hashCode(id); }
    }

    // author: soyoung0419, expected: 일기(날짜·텍스트·이미지) CRUD , developed: LocalDate/본문/이미지경로 보유 + 문자열 출력
    static class Diary implements Serializable {
        private static final long serialVersionUID = 1L;
        final long id;
        LocalDate date;
        String text;
        String imagePath; // 첨부 경로(옵션)
        Diary(long id, LocalDate date, String text, String imagePath) { this.id = id; this.date = date; this.text = text; this.imagePath = imagePath; }
        @Override
        public String toString() {
            String imgInfo = (imagePath != null && !imagePath.isBlank())
                    ? " (이미지 첨부)"
                    : "";

            return String.format("[#%d] %s | %s%s",
                    id,
                    date,
                    text,
                    imgInfo
            );
        }
        @Override public boolean equals(Object o){ if(this==o) return true; if(!(o instanceof Diary)) return false; return id==((Diary)o).id; }
        @Override public int hashCode(){ return Long.hashCode(id); }
    }

    // author: soyoung0419, expected: 일기 이미지 파일 관리(저장/열기/삭제) 기능, developed: images/ 폴더에 복사 저장 및 OS 기본 뷰어 열기
    static class ImageHelper {

        // images 폴더 생성
        static File ensureImageDir() {
            File dir = new File("images");
            if (!dir.exists()) dir.mkdirs();
            return dir;
        }

        static String randomName(String originalName) {
            String ext = "";
            int idx = originalName.lastIndexOf(".");
            if (idx != -1) ext = originalName.substring(idx);
            return UUID.randomUUID().toString() + ext;
        }

        // 이미지 파일 복사 후 저장 경로 반환
        static String saveImage(String srcPath) throws IOException {
            if (srcPath == null || srcPath.isBlank()) return null;

            File src = new File(srcPath);
            if (!src.exists()) throw new FileNotFoundException("이미지 파일이 존재하지 않습니다.");

            File dir = ensureImageDir();
            String newName = randomName(src.getName());
            File dst = new File(dir, newName);

            Files.copy(src.toPath(), dst.toPath(), StandardCopyOption.REPLACE_EXISTING);

            return "images/" + newName;   // 저장 경로
        }

        // Image open
        static void openImage(String path){
            if (path == null || path.isBlank()) {
                System.out.println("이미지가 없습니다.");
                return;
            }

            File f = new File(path);
            if (!f.exists()) {
                System.out.println("이미지 파일을 찾을 수 없습니다: " + path);
                return;
            }

            try {
                String os = System.getProperty("os.name").toLowerCase();
                Process p;

                if (os.contains("win")) {
                    // Windows
                    p = Runtime.getRuntime().exec(new String[]{"cmd", "/c", "start", "\"\"", path});
                } else if (os.contains("mac")) {
                    // macOS
                    p = Runtime.getRuntime().exec(new String[]{"open", path});
                } else {
                    // Linux
                    p = Runtime.getRuntime().exec(new String[]{"xdg-open", path});
                }

                p.waitFor();
            } catch (Exception e) {
                System.out.println("이미지 여는 중 오류: " + e.getMessage());
            }
        }

        // image delete
        static void deleteImage(String path) {
            if (path == null || path.isBlank()) return;

            File f = new File(path);
            if (f.exists()) {
                boolean ok = f.delete();
                if (!ok) {
                    System.out.println("이미지 삭제 실패(파일이 잠겨있을 수 있음): " + path);
                }
            }
        }


    }

    // author: hxeonsu, expected: 성적 엔트리 및 성적표시 , developed: 과목명/점수 + toString에서 등급 표시
    static class GradeEntry implements Serializable {
        private static final long serialVersionUID = 1L;
        final long id;
        String courseName;
        double score; // 0~100
        GradeEntry(long id, String courseName, double score){
            this.id = id; this.courseName = courseName; this.score = score;
        }
        @Override public String toString(){
            return String.format(Locale.ROOT, "[#%d] %s: %.2f점 (%s)", id, courseName, score, GradeViewModel.toLetter(score));
        }
        @Override public boolean equals(Object o){ if(this==o) return true; if(!(o instanceof GradeEntry)) return false; return id==((GradeEntry)o).id; }
        @Override public int hashCode(){ return Long.hashCode(id); }
    }

    // author: hxeonsu, expected: 목표 정의 , developed: 이름/카테고리/주당 목표 보관 및 출력
    static class Goal implements Serializable {
        private static final long serialVersionUID = 1L;
        final long id;
        String name;
        String category;     // 예: STUDY, LIFE, HEALTH 등 자유 입력
        int weeklyTarget;    // 주당 목표 횟수(1~7), 0이면 미사용

        Goal(long id, String name, String category, int weeklyTarget) {
            this.id = id;
            this.name = (name == null) ? "" : name;
            this.category = (category == null || category.isBlank()) ? "기타" : category;
            this.weeklyTarget = weeklyTarget;
        }

        @Override
        public String toString() {
            return String.format(Locale.ROOT,
                    "[#%d] %s (cat=%s, weeklyTarget=%d)",
                    id, name, category, weeklyTarget);
        }

        @Override
        public boolean equals(Object o){
            if (this == o) return true;
            if (!(o instanceof Goal)) return false;
            return id == ((Goal)o).id;
        }

        @Override
        public int hashCode(){
            return Long.hashCode(id);
        }
    }

    // author: hxeonsu, expected: 목표 완료 기록 보관 , developed: 날짜별 완료 여부 저장 및 streak 계산에 활용
    static class GoalRecord implements Serializable {
        private static final long serialVersionUID = 1L;
        long goalId;
        LocalDate date;
        boolean completed;

        GoalRecord(long goalId, LocalDate date, boolean completed) {
            this.goalId = goalId;
            this.date = date;
            this.completed = completed;
        }
    }


    // author: soyoung0419, expected: 전공 용어(용어/정의/카테고리) 관리 , developed: 난이도/태그/생성일/수정일 포함
    static class GlossaryEntry implements Serializable {
        private static final long serialVersionUID = 1L;

        long id;
        String term;          // 용어
        String definition;    // 설명
        String category;      // OS, DS, CN, DB, AI 등
        int difficulty;       // 1~5
        List<String> tags;    // 태그
        LocalDate created;
        LocalDate updated;

        GlossaryEntry(long id, String term, String definition,
                      String category, int difficulty, List<String> tags,
                      LocalDate created, LocalDate updated) {
            this.id = id;
            this.term = (term == null) ? "" : term.trim();
            this.definition = (definition == null) ? "" : definition.trim();
            this.category = (category == null || category.isBlank()) ? "기타" : category.trim();
            this.difficulty = difficulty;
            this.tags = (tags == null) ? new ArrayList<>() : new ArrayList<>(tags);
            this.created = (created == null) ? LocalDate.now() : created;
            this.updated = (updated == null) ? this.created : updated;
        }

        @Override
        public String toString() {
            String tagStr = (tags == null || tags.isEmpty())
                    ? "-"
                    : String.join(", ", tags);
            return String.format(Locale.ROOT,
                    "[#%d] %s (cat=%s, diff=%d) tags=[%s]\n  정의: %s\n  created=%s, updated=%s",
                    id, term, category, difficulty, tagStr,
                    definition,
                    created,
                    updated
            );
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof GlossaryEntry)) return false;
            return id == ((GlossaryEntry) o).id;
        }

        @Override
        public int hashCode() {
            return Long.hashCode(id);
        }
    }

        // author: jaeeun0109, expected: 과제(과목/제목/마감/제출여부) 구조 , developed: id/과목/제목/마감/제출방법/메모 + 문자열 표현
    static class Assignment implements Serializable {
        private static final long serialVersionUID = 1L;
        long id;
        String courseName;
        String title;
        LocalDateTime dueAt;
        boolean submitted;
        String submitMethod;
        String memo;

        Assignment(long id, String courseName, String title,
                   LocalDateTime dueAt, boolean submitted,
                   String submitMethod, String memo) {
            this.id = id;
            if (courseName == null) this.courseName = "";
            else this.courseName = courseName;

            if (title == null) this.title = "";
            else this.title = title;

            this.dueAt = dueAt;

            if (submitted == true) this.submitted = true;
            else this.submitted = false;

            if (submitMethod == null) this.submitMethod = "";
            else this.submitMethod = submitMethod;

            if (memo == null) this.memo = "";
            else this.memo = memo;
        }

        @Override
        public String toString() {
            String dueString;
            if (dueAt == null) {
                dueString = "-";
            } else {
                dueString = dueAt.format(DateTimeUtils.DATETIME_FMT);
            }

            String subString;
            if (submitted == true) subString = "true";
            else subString = "false";

            String memoString;
            if (memo == null || memo.trim().equals("")) memoString = "-";
            else memoString = memo;

            String course;
            if (courseName == null || courseName.trim().equals("")) course = "기타";
            else course = courseName;

            String method;
            if (submitMethod == null || submitMethod.trim().equals("")) method = "-";
            else method = submitMethod;

            String r = "";
            r = r + "[#" + id + "] ";
            r = r + course + " - " + title;
            r = r + " | due=" + dueString;
            r = r + " | submitted=" + subString;
            r = r + " | method=" + method;
            r = r + " | memo=" + memoString;
            String finalResult = new String(r);
            return finalResult;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Assignment)) return false;
            Assignment a = (Assignment) o;
            return this.id == a.id;
        }

        @Override
        public int hashCode() {
            String s = String.valueOf(id);
            return s.hashCode();
        }
    }
    

    // ====== Local Persistence ======
    // author: jaeeun0109, soyoung0419, hxeonsu, expected: 앱 전체 로컬 영속 저장 , developed: Java 직렬화 기반 in-file DB + 시퀀스 관리
    static class LocalStore implements Serializable {
        private static final long serialVersionUID = 1L;
        List<Task> tasks = new ArrayList<>();
        List<Schedule> schedules = new ArrayList<>();
        List<Friend> friends = new ArrayList<>();
        List<Diary> diaries = new ArrayList<>();
        List<GradeEntry> grades = new ArrayList<>();
        List<Task> archiveTasks = new ArrayList<>();
        List<Goal> goals = new ArrayList<>();
        List<GoalRecord> goalRecords = new ArrayList<>();
        List<GlossaryEntry> glossary = new ArrayList<>();
        List<Assignment> assignments = new ArrayList<>();

        long taskSeq = 1,
        scheduleSeq = 1,
        friendSeq = 1,
        diarySeq = 1,
        gradeSeq = 1,
        goalSeq = 1,
        glossarySeq = 1,
        assignmentSeq = 1;
    }

    // author: jaeeun0109, soyoung0419, hxeonsu, expected: 안전한 파일 저장/불러오기 , developed: tmp 파일 → ATOMIC_MOVE로 원자적 교체
    static class LocalDatabaseManager {
        private static final String FILE = "program_data.bin";
        private static final String TMP_FILE = "program_data.bin.tmp";

        synchronized LocalStore load() {
            File f = new File(FILE);
            if (!f.exists()) return new LocalStore();
            try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(f))) {
                Object obj = ois.readObject();
                if (obj instanceof LocalStore) return (LocalStore) obj;
            } catch (Exception e) {
                System.err.println("불러오기 실패: " + e.getMessage());
            }
            return new LocalStore();
        }

        synchronized void save(LocalStore store) {
            // 안전 저장: tmp에 먼저 쓰고 원자적 교체
            try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(TMP_FILE))) {
                oos.writeObject(store);
            } catch (IOException e) {
                System.err.println("저장 실패(임시파일 단계): " + e.getMessage());
                return;
            }
            try {
                Files.move(Paths.get(TMP_FILE), Paths.get(FILE), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (IOException e) {
                System.err.println("저장 실패(교체 단계): " + e.getMessage());
            }
        }
    }

    // ====== DAO-like layers working over LocalStore ======
    interface Crud<T> {
        T add(T t);
        Optional<T> get(long id);
        List<T> getAll();
        void update(T t);
        void delete(long id);
    }

    // author: jaeeun0109, expected: 할일 CRUD + 임박조회 , developed: add/update/delete/list + dueWithin 정렬 + 저장 연동
   static class TaskDao implements Crud<Task> {
        private final LocalStore store;
        private final LocalDatabaseManager db;

        TaskDao(LocalStore store, LocalDatabaseManager db) {
            if (store == null) {
                this.store = null;
            } else {
                this.store = store;
            }

            if (db == null) {
                this.db = null;
            } else {
                this.db = db;
            }
        }

        Task addNew(String title, LocalDateTime dueAt, String memo) {
            long id = store.taskSeq;
            store.taskSeq = store.taskSeq + 1;
            Task t = new Task(id, title, dueAt, false, memo);
            store.tasks.add(t);
            if (db != null) {
                db.save(store);
            } else {
                System.out.println("DB is null");
            }
            return t;
        }

        @Override
        public Task add(Task t) {
            if (t != null) {
                store.tasks.add(t);
                db.save(store);
            } else {
                System.out.println("null Task 추가 시도");
            }
            return t;
        }

        @Override
        public Optional<Task> get(long id) {
            for (Task x : store.tasks) {
                if (x.id == id) {
                    return Optional.of(x);
                }
            }
            return Optional.empty();
        }

        @Override
        public List<Task> getAll() {
            List<Task> copy = new ArrayList<>();
            for (Task t : store.tasks) {
                copy.add(t);
            }
            return copy;
        }

        @Override
        public void update(Task t) {
            Optional<Task> opt = get(t.id);
            if (opt.isPresent()) {
                Task old = opt.get();
                old.title = t.title;
                old.dueAt = t.dueAt;
                old.completed = t.completed;
                old.memo = t.memo;
                
                old.update(t.title, t.dueAt, t.memo, t.category, t.priority);
                
                if (db != null) {
                    db.save(store);
                }
            } else {
                System.out.println("업데이트할 Task 없음");
            }
        }

        @Override
        public void delete(long id) {
            Optional<Task> opt = get(id);
            if (opt.isPresent()) {
                Task x = opt.get();
                
                if (store.tasks.contains(x)) {
                    store.tasks.remove(x);
                    store.archiveTasks.add(x);
                } else {
                    System.out.println("삭제 실패: 리스트에 없음");
                }
                
                reassignTaskIds();
                
                if (db != null) {
                    db.save(store);
                }
            } else {
                System.out.println("삭제 대상 없음");
            }
        }

        private void reassignTaskIds() {
            long newId = 1;
            for (int i = 0; i < store.tasks.size(); i++) {
                Task t = store.tasks.get(i);
                try {
                    java.lang.reflect.Field f = Task.class.getDeclaredField("id");
                    f.setAccessible(true);
                    f.setLong(t, newId);
                    newId = newId + 1;
                } catch (Exception e) {
                    System.out.println("ID 재배정 중 오류 발생");
                    e.printStackTrace();
                }
            }
            store.taskSeq = newId;
        }

        List<Task> dueWithin(Duration d) {
            LocalDateTime now = LocalDateTime.now();
            List<Task> out = new ArrayList<>();
            for (int i = 0; i < store.tasks.size(); i++) {
                Task t = store.tasks.get(i);
                if (!t.completed) {
                    if (t.dueAt != null) {
                        Duration diff = Duration.between(now, t.dueAt);
                        if (diff.compareTo(d) <= 0) {
                            out.add(t);
                        }
                    }
                }
            }

            for (int i = 0; i < out.size(); i++) {
                for (int j = i + 1; j < out.size(); j++) {
                    Task a = out.get(i);
                    Task b = out.get(j);
                    if (a.dueAt != null && b.dueAt != null && a.dueAt.isAfter(b.dueAt)) {
                        out.set(i, b);
                        out.set(j, a);
                    }
                }
            }

            return out;
        }

       // 제목 검색
        List<Task> searchByTitle(String keyword) {
            List<Task> out = new ArrayList<>();
            for (Task t : store.tasks) {
                if (t.title != null && t.title.contains(keyword)) {
                    out.add(t);
                }
            }
            return out;
        }

       // 키워드 검색
        List<Task> searchByKeyword(String key) {
            List<Task> out = new ArrayList<>();
            for (Task t : store.tasks) {
                if ((t.title != null && t.title.contains(key)) ||
                    (t.memo != null && t.memo.contains(key))) {
                    out.add(t);
                }
            }
            return out;
        }

        // 기간 검색
        List<Task> searchByPeriod(LocalDate from, LocalDate to) {
            List<Task> out = new ArrayList<>();
            for (Task t : store.tasks) {
                if (t.dueAt != null) {
                    LocalDate d = t.dueAt.toLocalDate();
                    if (!d.isBefore(from) && !d.isAfter(to)) {
                        out.add(t);
                    }
                }
            }
            return out;
        }

        // 카테고리 필터
        List<Task> filterByCategory(String cat) {
            List<Task> out = new ArrayList<>();
            for (Task t : store.tasks) {
                if (t.category != null && t.category.equalsIgnoreCase(cat)) {
                    out.add(t);
                }
            }
            return out;
        }
        
        // 아카이브 반환
        List<Task> getArchive() {
        return store.archiveTasks;
        }
    }

    // author: soyoung0419, expected: 친구 CRUD , developed: add/update/delete/list 단순 DAO + 저장 동기화
    static class FriendDao implements Crud<Friend> {
        private final LocalStore store; private final LocalDatabaseManager db;
        FriendDao(LocalStore store, LocalDatabaseManager db){ this.store = store; this.db = db; }
        Friend addNew(String name, String uid){ 
            if (!uid.matches("^uid_[0-9]+$")) { throw new IllegalArgumentException("UID 형식 오류: uid_123 형태여야 합니다."); }
            for(Friend f : store.friends){ if(f.uid.equalsIgnoreCase(uid)) throw new IllegalArgumentException("이미 존재하는 UID입니다.");
        } long id = store.friendSeq++; Friend f = new Friend(id, name, uid); store.friends.add(f); db.save(store); return f; }
        @Override public Friend add(Friend f){ store.friends.add(f); db.save(store); return f; }
        @Override public Optional<Friend> get(long id){ return store.friends.stream().filter(x->x.id==id).findFirst(); }
        @Override public List<Friend> getAll(){ return new ArrayList<>(store.friends); }
        @Override public void update(Friend f){
            get(f.id).ifPresent(old -> {
                old.name = f.name;
                old.uid = f.uid;
                db.save(store);
            });
        }
        // 태그 변경 기능
        void updateTag(long id, String newTag) {
            for (Friend f : store.friends) {
                if (f.id == id) {
                    f.tag = newTag;
                    db.save(store);
                    return;
                }
            }
            throw new IllegalArgumentException("해당 ID의 친구가 없습니다.");
        }

        //  태그로 검색
        List<Friend> findByTag(String tag){
            List<Friend> result = new ArrayList<>();
            for (Friend f : store.friends){
                if (f.tag != null && f.tag.equalsIgnoreCase(tag)) {
                    result.add(f);
                }
            }
            return result;
        }
        @Override public void delete(long id){ get(id).ifPresent(x->{ store.friends.remove(x); db.save(store); }); }
    }

    // author: soyoung0419, expected: 일기 CRUD + 날짜별 조회 , developed: byDate/수정/삭제 구현 및 저장 동기화
    static class DiaryDao implements Crud<Diary> {
        private final LocalStore store; private final LocalDatabaseManager db;
        DiaryDao(LocalStore store, LocalDatabaseManager db){ this.store = store; this.db = db; }
        Diary addNew(LocalDate date, String text, String imagePath){
            try {
                imagePath = ImageHelper.saveImage(imagePath);  // 실제 복사
            } catch (Exception e){
                throw new RuntimeException("이미지 저장 실패: " + e.getMessage());
            }
            long id = store.diarySeq++;
            Diary d = new Diary(id, date, text, imagePath);
            store.diaries.add(d);
            db.save(store);
            return d;
        }
        @Override public Diary add(Diary d){ store.diaries.add(d); db.save(store); return d; }
        @Override public Optional<Diary> get(long id){ return store.diaries.stream().filter(x->x.id==id).findFirst(); }
        @Override public List<Diary> getAll(){ return new ArrayList<>(store.diaries); }
        public void update(Diary d){
            get(d.id).ifPresent(old -> {
                old.date = d.date;
                old.text = d.text;
                
                if ("__DELETE_IMAGE__".equals(d.imagePath)) {
                    ImageHelper.deleteImage(old.imagePath);
                    old.imagePath = null;
                }
    
                else if (d.imagePath != null && !d.imagePath.isBlank()) {
                    try {
                        ImageHelper.deleteImage(old.imagePath);
                        old.imagePath = ImageHelper.saveImage(d.imagePath);

                    } catch (Exception e){
                        throw new RuntimeException("이미지 저장 실패: " + e.getMessage());
                    }
                }
                db.save(store);
            });
        }
        @Override public void delete(long id){ get(id).ifPresent(x->{ store.diaries.remove(x); db.save(store); }); }
        List<Diary> byDate(LocalDate date){
            List<Diary> l = new ArrayList<>();
            for (Diary d: store.diaries) if (DateTimeUtils.isSameDay(d.date, date)) l.add(d);
            return l;
        }
        
        List<Diary> searchByKeyword(String keyword){
            List<Diary> result = new ArrayList<>();
            for (Diary d : store.diaries){
                if (d.text != null && d.text.contains(keyword)) {
                    result.add(d);
                }
            }
            return result;
        }

    }
    

    // author: hxeonsu, expected: 목표 DAO , developed: Goal CRUD 및 날짜별 완료 기록 토글/통계 제공
    static class GoalDao implements Crud<Goal> {
        private final LocalStore store;
        private final LocalDatabaseManager db;

        GoalDao(LocalStore store, LocalDatabaseManager db) {
            this.store = store;
            this.db = db;

            // 기존 저장 파일과의 호환성을 위한 null 방어
            if (this.store.goals == null) this.store.goals = new ArrayList<>();
            if (this.store.goalRecords == null) this.store.goalRecords = new ArrayList<>();
            if (this.store.goalSeq <= 0) this.store.goalSeq = 1;
        }

        Goal addNew(String name, String category, int weeklyTarget) {
            long id = store.goalSeq++;
            Goal h = new Goal(id, name, category, weeklyTarget);
            store.goals.add(h);
            if (db != null) db.save(store);
            return h;
        }

        @Override
        public Goal add(Goal h) {
            store.goals.add(h);
            if (db != null) db.save(store);
            return h;
        }

        @Override
        public Optional<Goal> get(long id) {
            for (Goal h : store.goals) {
                if (h.id == id) return Optional.of(h);
            }
            return Optional.empty();
        }

        @Override
        public List<Goal> getAll() {
            return new ArrayList<>(store.goals);
        }

        @Override
        public void update(Goal h) {
            get(h.id).ifPresent(old -> {
                old.name = h.name;
                old.category = h.category;
                old.weeklyTarget = h.weeklyTarget;
                if (db != null) db.save(store);
            });
        }

        @Override
        public void delete(long id) {
            get(id).ifPresent(h -> {
                store.goals.remove(h);
                // 해당 목표의 기록도 함께 삭제
                store.goalRecords.removeIf(r -> r.goalId == id);
                if (db != null) db.save(store);
            });
        }

        // 특정 날짜의 완료 여부
        boolean isCompletedOnDate(long goalId, LocalDate date) {
            for (GoalRecord r : store.goalRecords) {
                if (r.goalId == goalId && DateTimeUtils.isSameDay(r.date, date) && r.completed) {
                    return true;
                }
            }
            return false;
        }

        // 해당 날짜의 기록 토글 (있으면 삭제, 없으면 완료로 추가)
        void toggleCompletion(long goalId, LocalDate date) {
            Iterator<GoalRecord> it = store.goalRecords.iterator();
            while (it.hasNext()) {
                GoalRecord r = it.next();
                if (r.goalId == goalId && DateTimeUtils.isSameDay(r.date, date)) {
                    it.remove(); // 토글 OFF
                    if (db != null) db.save(store);
                    return;
                }
            }
            // 기존 기록 없으면 완료 기록 추가
            store.goalRecords.add(new GoalRecord(goalId, date, true));
            if (db != null) db.save(store);
        }

        // 기간 내 완료 횟수
        long countCompletedInRange(long goalId, LocalDate from, LocalDate to) {
            long count = 0;
            for (GoalRecord r : store.goalRecords) {
                if (r.goalId == goalId && r.completed) {
                    if ((!r.date.isBefore(from)) && (!r.date.isAfter(to))) {
                        count++;
                    }
                }
            }
            return count;
        }

        // base 날짜 기준 연속 완료 일수(streak)
        int calcStreak(long goalId, LocalDate base) {
            int streak = 0;
            LocalDate d = base;
            while (true) {
                if (isCompletedOnDate(goalId, d)) {
                    streak++;
                    d = d.minusDays(1);
                } else {
                    break;
                }
            }
            return streak;
        }
    }

    
    // author: soyoung0419, expected: 전공 용어 DAO , developed: CRUD + 검색/카테고리/정렬/CSV export 제공
    static class GlossaryDao implements Crud<GlossaryEntry> {
        private final LocalStore store;
        private final LocalDatabaseManager db;

        GlossaryDao(LocalStore store, LocalDatabaseManager db) {
            this.store = store;
            this.db = db;
            if (this.store.glossary == null) this.store.glossary = new ArrayList<>();
            if (this.store.glossarySeq <= 0) this.store.glossarySeq = 1;
        }

        GlossaryEntry addNew(String term, String def, String category, int difficulty, List<String> tags) {
            long id = store.glossarySeq++;
            GlossaryEntry e = new GlossaryEntry(id, term, def, category, difficulty, tags, LocalDate.now(), LocalDate.now());
            store.glossary.add(e);
            if (db != null) db.save(store);
            return e;
        }

        @Override
        public GlossaryEntry add(GlossaryEntry e) {
            store.glossary.add(e);
            if (db != null) db.save(store);
            return e;
        }

        @Override
        public Optional<GlossaryEntry> get(long id) {
            for (GlossaryEntry e : store.glossary) {
                if (e.id == id) return Optional.of(e);
            }
            return Optional.empty();
        }

        @Override
        public List<GlossaryEntry> getAll() {
            return new ArrayList<>(store.glossary);
        }

        @Override
        public void update(GlossaryEntry e) {
            get(e.id).ifPresent(old -> {
                old.term = e.term;
                old.definition = e.definition;
                old.category = e.category;
                old.difficulty = e.difficulty;
                old.tags = new ArrayList<>(e.tags);
                old.updated = LocalDate.now();
                if (db != null) db.save(store);
            });
        }

        @Override
        public void delete(long id) {
            get(id).ifPresent(x -> {
                store.glossary.remove(x);
                if (db != null) db.save(store);
            });
        }

        List<GlossaryEntry> searchByTerm(String keyword) {
            List<GlossaryEntry> out = new ArrayList<>();
            if (keyword == null || keyword.isBlank()) return out;
            String kw = keyword.toLowerCase(Locale.ROOT);
            for (GlossaryEntry e : store.glossary) {
                if (e.term.toLowerCase(Locale.ROOT).contains(kw)
                        || e.definition.toLowerCase(Locale.ROOT).contains(kw)) {
                    out.add(e);
                }
            }
            return out;
        }

        List<GlossaryEntry> byCategory(String category) {
            List<GlossaryEntry> out = new ArrayList<>();
            for (GlossaryEntry e : store.glossary) {
                if (e.category != null && e.category.equalsIgnoreCase(category)) {
                    out.add(e);
                }
            }
            return out;
        }

        List<GlossaryEntry> sorted(String mode) {
            List<GlossaryEntry> list = getAll();
            if (mode == null) mode = "name";
            switch (mode) {
                case "recent":
                    list.sort(Comparator.comparing((GlossaryEntry e) -> e.created).reversed());
                    break;
                case "diff":
                    list.sort(Comparator.comparingInt(e -> e.difficulty));
                    break;
                case "category":
                    list.sort(Comparator
                            .comparing((GlossaryEntry e) -> e.category.toLowerCase(Locale.ROOT))
                            .thenComparing(e -> e.term.toLowerCase(Locale.ROOT)));
                    break;
                case "name":
                default:
                    list.sort(Comparator.comparing(e -> e.term.toLowerCase(Locale.ROOT)));
                    break;
            }
            return list;
        }

        GlossaryEntry randomOne(Random rnd) {
            if (store.glossary == null || store.glossary.isEmpty()) return null;
            int idx = rnd.nextInt(store.glossary.size());
            return store.glossary.get(idx);
        }

        List<GlossaryEntry> randomOptions(Random rnd, GlossaryEntry answer, int count) {
            List<GlossaryEntry> all = new ArrayList<>(store.glossary);
            if (answer != null) all.remove(answer);
            Collections.shuffle(all, rnd);
            List<GlossaryEntry> opts = new ArrayList<>();
            opts.add(answer);
            for (int i = 0; i < all.size() && opts.size() < count; i++) {
                opts.add(all.get(i));
            }
            Collections.shuffle(opts, rnd);
            return opts;
        }

        void exportCsv(String fileName) {
            if (fileName == null || fileName.isBlank()) fileName = "glossary_export.csv";
            try (BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(fileName), java.nio.charset.StandardCharsets.UTF_8))) {

                // UTF-8 BOM 
                bw.write("\uFEFF");
                
                bw.write("id,term,definition,category,difficulty,tags,created,updated");
                bw.newLine();
                for (GlossaryEntry e : store.glossary) {
                    String tagsStr = (e.tags == null || e.tags.isEmpty())
                            ? ""
                            : String.join(";", e.tags);
                    bw.write(csvEscape(Long.toString(e.id)) + "," +
                             csvEscape(e.term) + "," +
                             csvEscape(e.definition) + "," +
                             csvEscape(e.category) + "," +
                             csvEscape(Integer.toString(e.difficulty)) + "," +
                             csvEscape(tagsStr) + "," +
                             csvEscape(e.created.toString()) + "," +
                             csvEscape(e.updated.toString()));
                    bw.newLine();
                }
                bw.flush();
                System.out.println("CSV 내보내기 완료: " + fileName);
            } catch (IOException ex) {
                System.out.println("CSV 내보내기 실패: " + ex.getMessage());
            }
        }

        private static String csvEscape(String s) {
            if (s == null) return "";
            String r = s.replace("\"", "\"\"");
            if (r.contains(",") || r.contains("\"") || r.contains("\n")) {
                return "\"" + r + "\"";
            }
            return r;
        }
    }



    
    // ====== Services / ViewModels ======
    // author: soyoung0419, expected: 친구 관리 뷰모델 , developed: add/list/delete 단순 위임
    static class FriendViewModel {
        private final FriendDao dao;
        FriendViewModel(FriendDao dao){ this.dao = dao; }
        Friend add(String name, String uid){ return dao.addNew(name, uid); }
        void delete(long id){ dao.delete(id); }
        List<Friend> list(){ return dao.getAll(); }
        
        void update(long id, String newName, String newUid){
            dao.get(id).ifPresent(f -> {
                if (newName != null && !newName.isBlank()) f.name = newName;
                if (newUid != null && !newUid.isBlank()) f.uid = newUid;
                dao.update(f);
            });
        }
        
        //  태그 수정
        void updateTag(long id, String tag){
            dao.updateTag(id, tag);
        }

        //  태그 검색
        List<Friend> findByTag(String tag){
            return dao.findByTag(tag);
        }

    }

    // author: soyoung0419, expected: 달력/일기 통합(일기+해당 날짜 할일 보기) , developed: diariesOn/tasksDueOn/수정/삭제
    static class CalendarViewModel {
        private final DiaryDao diaries;
        private final TaskDao tasks;

        CalendarViewModel(DiaryDao diaries, TaskDao tasks){
            this.diaries = diaries;
            this.tasks = tasks;
        }

        Diary addDiary(LocalDate date, String text, String image){
            return diaries.addNew(date, text, image);
        }

        List<Diary> diariesOn(LocalDate date){ return diaries.byDate(date); }

        List<Task> tasksDueOn(LocalDate date){
            List<Task> out = new ArrayList<>();
            for(Task t: tasks.getAll())
                if (t.dueAt!=null && DateTimeUtils.isSameDay(t.dueAt.toLocalDate(), date))
                    out.add(t);
            return out;
        }

        List<Diary> getAllDiaries() {
            List<Diary> all = diaries.getAll();

            // 날짜 기준 정렬
            all.sort(Comparator.comparing(d -> d.date));
            return all;
        }

        List<Diary> searchKeyword(String keyword){
            return diaries.searchByKeyword(keyword);
        }

        void updateDiary(long id, String newText, String newImagePath) {
            diaries.get(id).ifPresent(d -> {

                if (newText != null && !newText.isBlank())
                    d.text = newText;
                
                if ("__DELETE_IMAGE__".equals(newImagePath)) {
                    d.imagePath = "__DELETE_IMAGE__";
                }
                
                else if (newImagePath != null && !newImagePath.isBlank()) {
                    d.imagePath = newImagePath;
                }
                diaries.update(d);
            });
        }
        void deleteDiary(long id){ diaries.delete(id); }
    }
    



    // author: hxeonsu, expected: 목표 트래커 비즈니스 로직 , developed: CRUD, 오늘 완료 토글, 7일/30일 통계, streak 계산
    static class GoalViewModel {
        private final GoalDao dao;

        static class GoalStats {
            long last7;
            long last30;
            int streak;

            GoalStats(long last7, long last30, int streak) {
                this.last7 = last7;
                this.last30 = last30;
                this.streak = streak;
            }
        }

        GoalViewModel(GoalDao dao) {
            this.dao = dao;
        }

        Goal add(String name, String category, int weeklyTarget) {
            if (name == null || name.isBlank()) {
                throw new IllegalArgumentException("목표 이름은 비어 있을 수 없습니다.");
            }
            if (weeklyTarget < 0 || weeklyTarget > 7) {
                throw new IllegalArgumentException("주당 목표는 0~7 사이여야 합니다.");
            }
            return dao.addNew(name, category, weeklyTarget);
        }

        void delete(long id) {
            dao.delete(id);
        }

        List<Goal> list() {
            List<Goal> list = dao.getAll();
            list.sort(Comparator.comparing(h -> h.name.toLowerCase(Locale.ROOT)));
            return list;
        }

        void toggleToday(long goalId, LocalDate today) {
            dao.toggleCompletion(goalId, today);
        }

        GoalStats stats(long goalId, LocalDate base) {
            LocalDate last7From = base.minusDays(6);
            LocalDate last30From = base.minusDays(29);
            long c7 = dao.countCompletedInRange(goalId, last7From, base);
            long c30 = dao.countCompletedInRange(goalId, last30From, base);
            int streak = dao.calcStreak(goalId, base);
            return new GoalStats(c7, c30, streak);
        }

        boolean isCompletedOn(long goalId, LocalDate date) {
            return dao.isCompletedOnDate(goalId, date);
        }

        int streak(long goalId, LocalDate date) {
            return dao.calcStreak(goalId, date);
        }

        int totalCount() {
            return dao.getAll().size();
        }

        int completedCountOn(LocalDate date) {
            int c = 0;
            for (Goal h : dao.getAll()) {
                if (dao.isCompletedOnDate(h.id, date)) c++;
            }
            return c;
        }
    }

    
    // author: soyoung0419, expected: 전공 용어 비즈니스 로직 , developed: 중복 방지/검증/검색/정렬/퀴즈/CSV export
    static class GlossaryViewModel {
        private final GlossaryDao dao;

        GlossaryViewModel(GlossaryDao dao) {
            this.dao = dao;
        }

        private static List<String> parseTags(String tagInput) {
            List<String> tags = new ArrayList<>();
            if (tagInput == null || tagInput.isBlank()) return tags;
            String[] parts = tagInput.split(",");
            for (String p : parts) {
                String t = p.trim();
                if (!t.isEmpty()) tags.add(t);
            }
            return tags;
        }

        GlossaryEntry add(String term, String def, String category, int difficulty, String tagInput) {
            if (term == null || term.isBlank())
                throw new IllegalArgumentException("용어는 비어 있을 수 없습니다.");
            if (def == null || def.isBlank())
                throw new IllegalArgumentException("정의는 비어 있을 수 없습니다.");
            if (difficulty < 1 || difficulty > 5)
                throw new IllegalArgumentException("난이도는 1~5 사이여야 합니다.");

            // 중복 용어 방지 (대소문자 무시)
            String lower = term.toLowerCase(Locale.ROOT);
            for (GlossaryEntry e : dao.getAll()) {
                if (e.term.toLowerCase(Locale.ROOT).equals(lower)) {
                    throw new IllegalArgumentException("이미 등록된 용어입니다: " + term);
                }
            }

            List<String> tags = parseTags(tagInput);
            return dao.addNew(term, def, category, difficulty, tags);
        }

        void update(long id, String newTerm, String newDef, String newCat,
                    Integer newDiff, String newTagsInput) {
            dao.get(id).ifPresentOrElse(e -> {
                if (newTerm != null && !newTerm.isBlank()) e.term = newTerm.trim();
                if (newDef != null && !newDef.isBlank()) e.definition = newDef.trim();
                if (newCat != null && !newCat.isBlank()) e.category = newCat.trim();
                if (newDiff != null) {
                    if (newDiff < 1 || newDiff > 5)
                        throw new IllegalArgumentException("난이도는 1~5 사이여야 합니다.");
                    e.difficulty = newDiff;
                }
                if (newTagsInput != null) {
                    e.tags = parseTags(newTagsInput);
                }
                e.updated = LocalDate.now();
                dao.update(e);
            }, () -> {
                throw new IllegalArgumentException("해당 ID의 용어가 없습니다.");
            });
        }

        void delete(long id) {
            dao.delete(id);
        }

        List<GlossaryEntry> search(String keyword) {
            return dao.searchByTerm(keyword);
        }

        List<GlossaryEntry> byCategory(String category) {
            return dao.byCategory(category);
        }

        List<GlossaryEntry> listSorted(String mode) {
            return dao.sorted(mode);
        }

        GlossaryEntry randomOne(Random rnd) {
            return dao.randomOne(rnd);
        }

        List<GlossaryEntry> randomOptions(Random rnd, GlossaryEntry answer, int count) {
            return dao.randomOptions(rnd, answer, count);
        }

        void exportCsv(String fileName) {
            dao.exportCsv(fileName);
        }

        List<GlossaryEntry> all() {
            return dao.getAll();
        }
    }

  

    // ====== Console App (Main) ======
    public static void main(String[] args) {
        System.out.println("= CampusPlanner Console (통합/Local DB) =");

        // Load persisted store
        LocalDatabaseManager db = new LocalDatabaseManager();
        LocalStore store = db.load();

        // author: hxeonsu, expected: 기존 저장 파일과 Goal 필드 호환성 확보 , developed: null/0 방어 초기화
        if (store.goals == null) store.goals = new ArrayList<>();
        if (store.goalRecords == null) store.goalRecords = new ArrayList<>();
        if (store.goalSeq <= 0) store.goalSeq = 1;

        // author: soyoung0419, expected: glossary 필드 누락 시 로딩 오류 방지 , developed: 목록과 시퀀스 기본값 설정
        if (store.glossary == null) store.glossary = new ArrayList<>();
        if (store.glossarySeq <= 0) store.glossarySeq = 1;

        // author: jaeeun0109, expected: 과제 필드 누락 시 로딩 오류 방지 , developed: 목록과 시퀀스 기본값 설정
        if (store.assignments == null) store.assignments = new ArrayList<>();
        if (store.assignmentSeq <= 0) store.assignmentSeq = 1;

        // Wiring
        TaskDao taskDao = new TaskDao(store, db);
        ScheduleDao scheduleDao = new ScheduleDao(store, db);
        FriendDao friendDao = new FriendDao(store, db);
        DiaryDao diaryDao = new DiaryDao(store, db);
        GradeDao gradeDao = new GradeDao(store, db);
        GoalDao goalDao = new GoalDao(store, db);
        GlossaryDao glossaryDao = new GlossaryDao(store, db);
        AssignmentDao assignmentDao = new AssignmentDao(store, db);
        NotificationHelper notifier = new NotificationHelper();

        TaskViewModel taskVM = new TaskViewModel(taskDao, notifier);
        ScheduleViewModel schedVM = new ScheduleViewModel(scheduleDao);
        FriendViewModel friendVM = new FriendViewModel(friendDao);
        CalendarViewModel calVM = new CalendarViewModel(diaryDao, taskDao);
        GradeViewModel gradeVM = new GradeViewModel(gradeDao, schedVM);
        GoalViewModel goalVM = new GoalViewModel(goalDao);
        GlossaryViewModel glossaryVM = new GlossaryViewModel(glossaryDao);
        AssignmentViewModel assignmentVM = new AssignmentViewModel(assignmentDao);
    
        // Seed only when empty (두 코드의 시드 보존)
        // author: jaeeun0109, expected: 초기 데이터에 할일/시간표 제공 , developed: task/schedule 기본값 시드
        // author: soyoung0419, expected: 초기 데이터에 친구/일기 제공 , developed: friends/diary 기본값 시드
        if (store.tasks.isEmpty() && store.schedules.isEmpty() && store.friends.isEmpty() && store.diaries.isEmpty()) {
            seed(taskVM, schedVM, friendVM, calVM);
            db.save(store);
        }

        // 재시작 시 알림 재등록
        // author: jaeeun0109, expected: 앱 재시작 시 마감 알림 복원 , developed: rebookAllAlarms 호출로 재예약
        taskVM.rebookAllAlarms();

        try(Scanner sc = new Scanner(System.in)){
            loop: while(true){
                printMenu();
                String cmd = sc.nextLine().trim();
                try {
                    switch (cmd) {
                        case "1": handleTasks(sc, taskVM); break;      // author: jaeeun0109, expected: 할일 메뉴 처리 , developed: a/l/t/d/s 명령 처리
                        case "2": handleSchedule(sc, schedVM); break;   // author: jaeeun0109, expected: 시간표 메뉴 처리 , developed: a/l/n/d/day 명령 처리
                        case "3": handleFriends(sc, friendVM); break;   // author: soyoung0419, expected: 친구 메뉴 처리 , developed: a/l/d 명령 처리
                        case "4": handleDiary(sc, calVM); break;        // author: soyoung0419, expected: 다이어리 메뉴 처리 , developed: a/l/all/d/u 명령 처리
                        case "5": handleToday(calVM, goalVM, assignmentVM); break;   // author: hxeonsu, expected: 오늘보기 + 목표 요약 , developed: 오늘 일기/마감할일/목표 통합 출력
                        case "6": handleGrade(sc, gradeVM, schedVM); break; // author: hxeonsu, expected: 성적 메뉴 , developed: a/u/d/l/avg/gpa/sum/course
                        case "7": handleGoal(sc, goalVM); break;      // author: hxeonsu, expected: 목표 메뉴 처리 , developed: a/l/t/d/s/today 명령 처리
                        case "8": handleGlossary(sc, glossaryVM); break;     // author: soyoung0419, expected: 전공 용어 사전 메뉴 처리 , developed: a/e/d/s/c/l/q/csv/b 명령 처리
                        case "9": handleAssignment(sc, assignmentVM); break; // author: jaeeun0109, expected: 과제 메뉴 , developed: a/l/t/d/s/f/k/n 명령 처리
                        case "0": break loop;
                        default: System.out.println("알 수 없는 명령");
                    }
                } catch (Exception e){
                    System.out.println("오류: " + e.getMessage());
                }
            }
        }
        // author: jaeeun0109, soyoung0419, hxeonsu, expected: 종료 시 자원 정리 , developed: 알림 스케줄러 종료 및 저장
        notifier.shutdown();
        // Auto-save at exit
        db.save(store);
        System.out.println("종료 (데이터 저장됨)");
    }

    static void printMenu(){
        // author: jaeeun0109, soyoung0419, hxeonsu, expected: 메인 메뉴 표시 , developed: 콘솔 출력 UI
        System.out.println();
        System.out.println("========================================");
        System.out.println("                 [메뉴]");
        System.out.println("========================================");
        System.out.println(" 1: 할일");
        System.out.println(" 2: 시간표");
        System.out.println(" 3: 친구");
        System.out.println(" 4: 다이어리");
        System.out.println(" 5: 오늘보기");
        System.out.println(" 6: 성적");
        System.out.println(" 7: 목표 트래커");
        System.out.println(" 8: 전공 용어 사전");
        System.out.println(" 9: 과제 제출 관리");
        System.out.println(" 0: 종료");
        System.out.println("----------------------------------------");
        System.out.print("> ");
    }

    // === Handlers ===




    // author: soyoung0419, expected: 친구 인터랙션(추가/목록/삭제) , developed: FriendViewModel 호출 로직
    static void handleFriends(Scanner sc, FriendViewModel vm){
        while(true){
            System.out.println("========================================");
            System.out.println("                 [친구]");
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
                switch (c){
                    case "a":
                    while (true) {  
                        try { 
                            System.out.print("이름: "); 
                            String name = sc.nextLine(); 
                            
                            System.out.print("UID (형식: uid_숫자): "); 
                            String uid = sc.nextLine(); 
                            
                            Friend f = vm.add(name, uid);  
                            System.out.println("추가됨: " + f); break; 
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
                        
                    case "l": vm.list().forEach(System.out::println); break;
                    
                    case "u":  // 친구 정보 수정 
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

                    case "d":  // 친구 삭제
                        System.out.print("삭제할 ID: ");
                        long delId = Long.parseLong(sc.nextLine());
                        vm.delete(delId);
                        System.out.println("삭제됨");
                        break;

                    case "t":  // 태그 수정 
                        System.out.print("태그 수정할 ID: ");
                        long tagId = Long.parseLong(sc.nextLine());

                        System.out.print("새 태그 입력(예: 동기/팀플/선배/후배): ");
                        String tag = sc.nextLine();

                        vm.updateTag(tagId, tag);
                        System.out.println("태그 수정 완료");
                        break;

                    case "ft": // 태그 검색 
                        System.out.print("검색할 태그 입력: ");
                        String search = sc.nextLine();

                        List<Friend> filtered = vm.findByTag(search);
                        if (filtered.isEmpty())
                            System.out.println("해당 태그의 친구가 없습니다.");
                        else
                            filtered.forEach(System.out::println);
                        break;

                    default: System.out.println("? (알 수 없는 명령)");
                } 
            } catch (Exception e){
                        System.out.println("오류: " + e.getMessage());
                }       
            }
    }

    // author: soyoung0419, expected: 다이어리 인터랙션(추가/날짜조회/전체/삭제/수정) , developed: CalendarViewModel 통해 일기 및 해당일 할일 조회
    static void handleDiary(Scanner sc, CalendarViewModel vm){
        while(true){
            System.out.println("========================================");
            System.out.println("               [다이어리]");
            System.out.println("========================================");
            System.out.println(" a     : 추가");
            System.out.println(" l     : 날짜별 조회");
            System.out.println(" all   : 전체 목록");
            System.out.println(" u     : 수정");
            System.out.println(" d     : 삭제");
            System.out.println(" img   : 이미지 열기");
            System.out.println(" imgdel: 이미지 삭제");
            System.out.println(" k     : 키워드 검색");
            System.out.println(" b     : 뒤로가기");
            System.out.println("----------------------------------------");
            System.out.print("> ");

            String c = sc.nextLine().trim();
            if (c.equals("b")) return;
            switch (c){
                case "a":
                    System.out.print("날짜(yyyy-MM-dd): "); LocalDate d = DateTimeUtils.parseDate(sc.nextLine());
                    System.out.print("내용: "); String text = sc.nextLine();
                    System.out.print("이미지 경로(선택): "); String img = sc.nextLine();
                    Diary dd = vm.addDiary(d, text, img.isBlank()? null: img);
                    System.out.println("추가됨: " + dd);
                    break;
                    
                case "all":
                    System.out.println("[전체 일기 목록]");
                    vm.getAllDiaries().forEach(System.out::println);
                    break;
                    
                case "l":
                    System.out.print("날짜(yyyy-MM-dd): "); LocalDate date = DateTimeUtils.parseDate(sc.nextLine());
                    System.out.println("[일기]"); vm.diariesOn(date).forEach(System.out::println);
                    System.out.println("[해당 날짜 마감 할일]"); vm.tasksDueOn(date).forEach(System.out::println);
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
                    
                    System.out.print("새 내용 (변경하지 않으려면 Enter): ");
                    String newText = sc.nextLine();
                    
                    System.out.print("새 이미지 경로 (변경하지 않으려면 Enter): ");
                    String newImg = sc.nextLine();
                    
                    vm.updateDiary(uid, newText, newImg.isBlank() ? null : newImg);
                    System.out.println("수정 완료");
                    break;

                case "img":
                    System.out.print("이미지 볼 일기 ID: ");
                    long imgId = Long.parseLong(sc.nextLine());
                    vm.getAllDiaries().stream()
                            .filter(di -> di.id == imgId)
                            .findFirst()
                            .ifPresentOrElse(
                                di -> ImageHelper.openImage(di.imagePath),
                                () -> System.out.println("해당 ID의 일기를 찾을 수 없습니다.")
                            );
                    break;

                case "imgdel":
                    System.out.print("이미지 삭제할 일기 ID: ");
                    long delImgId = Long.parseLong(sc.nextLine());
                    vm.getAllDiaries().stream()
                            .filter(di -> di.id == delImgId)
                            .findFirst()
                            .ifPresentOrElse(
                                di -> {
                                    try {
                                        vm.updateDiary(di.id, di.text, "__DELETE_IMAGE__");
                                        System.out.println("이미지 삭제 완료");
                                    } catch (Exception e){
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

                    if (found.isEmpty())
                        System.out.println("해당 키워드가 포함된 일기가 없습니다.");
                    else
                        found.forEach(System.out::println);
                    break;
                default: System.out.println("?");
            }
        }
    }


    
    // author: hxeonsu, expected: 오늘 보기(일기/할일/목표 요약) , developed: 날짜 기준 요약 출력 및 빈 목록 안내 + GoalViewModel와 연동해 오늘 목표 달성 현황 출력
    // author: jaeeun0109, expected: 과제 표시만 추가, developed: 오늘 날짜 기준으로 마감되는 과제를 AssignmentViewModel에서 조회하여 [과제(오늘 마감)] 섹션에 출력하도록 기능 추가
    static void handleToday(CalendarViewModel vm, GoalViewModel goalVM, AssignmentViewModel assignVM){
        LocalDate today = LocalDate.now();
        System.out.println("========================================");
        System.out.println("         [오늘보기(" + today + ")]");
        System.out.println("========================================");
        List<Task> due = vm.tasksDueOn(today);
        List<Diary> ds = vm.diariesOn(today);

        List<Assignment> dueAssign = new ArrayList<>();
        for (Assignment a : assignVM.list()) {
            if (a.dueAt != null && DateTimeUtils.isSameDay(a.dueAt.toLocalDate(), today)) {
                dueAssign.add(a);
            }
        }

        int totalGoals = goalVM.totalCount();
        int doneGoals = goalVM.completedCountOn(today);

        System.out.println("[요약]");
        System.out.println(" - 오늘 마감할 일: " + due.size() + "개");
        System.out.println(" - 오늘 마감 과제: " + dueAssign.size() + "개");
        System.out.println(" - 오늘 작성한 일기: " + ds.size() + "개");

        System.out.println("[할일(오늘 마감)]");
        if (due.isEmpty()) System.out.println(" (없음)");
        else due.forEach(System.out::println);

        System.out.println("[과제(오늘 마감)]");
        if (dueAssign.isEmpty()) System.out.println(" (없음)");
        else dueAssign.forEach(System.out::println);

        System.out.println("[일기]");
        if (ds.isEmpty()) System.out.println(" (없음)");
        else ds.forEach(System.out::println);

        System.out.println("[목표]");
        if (totalGoals == 0) {
            System.out.println(" (없음)");
        } else {
            LocalDate base = today;
            for (Goal h : goalVM.list()) {
                boolean completed = goalVM.isCompletedOn(h.id, today);
                int streak = goalVM.streak(h.id, base);
                String status = completed ? "완료" : "미완료";
                System.out.println(String.format(Locale.ROOT,
                        " [#%d] %s (cat=%s) - 오늘: %s, 연속 %d일",
                        h.id, h.name, h.category, status, streak));
            }
        }

        if (totalGoals == 0) {
            System.out.println(" → 오늘 목표 달성률: ");
        } else {
            double rate = (doneGoals * 100.0) / totalGoals;
            int rateInt = (int)Math.round(rate);
            System.out.println(" → 오늘 목표 달성률: " 
                               + rateInt + "% (" + doneGoals + "/" + totalGoals + "개)");
        }

        System.out.println("----------------------------------------");
    }



    // author: hxeonsu, expected: 목표 메뉴 인터랙션 , developed: 추가/목록/오늘토글/삭제/통계/오늘현황 출력
    static void handleGoal(Scanner sc, GoalViewModel vm) {
        while (true) {
            System.out.println("========================================");
            System.out.println("              [목표 트래커]");
            System.out.println("========================================");
            System.out.println(" a    : 목표 추가");
            System.out.println(" l    : 목표 목록");
            System.out.println(" t    : 오늘 완료 토글");
            System.out.println(" d    : 삭제");
            System.out.println(" s    : 통계(7일/30일/연속일)");
            System.out.println(" today: 오늘 진행 상황");
            System.out.println(" b    : 뒤로가기");
            System.out.println("----------------------------------------");
            System.out.print("> ");

            String c = sc.nextLine().trim();
            if (c.equals("b")) return;

            try {
                switch (c) {
                    case "a":
                        System.out.print("목표 이름: ");
                        String name = sc.nextLine();
                        System.out.print("카테고리: ");
                        String cat = sc.nextLine();
                        System.out.print("주당 목표 횟수(1~7): ");
                        String wt = sc.nextLine().trim();
                        int weeklyTarget = wt.isEmpty() ? 0 : Integer.parseInt(wt);
                        Goal h = vm.add(name, cat, weeklyTarget);
                        System.out.println("추가됨: " + h);
                        break;

                    case "l":
                        List<Goal> list = vm.list();
                        if (list.isEmpty()) System.out.println("(등록된 목표 없음)");
                        else list.forEach(System.out::println);
                        break;

                    case "t":
                        System.out.print("오늘 완료 토글할 습관 ID: ");
                        long hid = Long.parseLong(sc.nextLine());
                        vm.toggleToday(hid, LocalDate.now());
                        System.out.println("오늘 완료 상태가 토글되었습니다.");
                        break;

                    case "d":
                        System.out.print("삭제할 목표 ID: ");
                        long delId = Long.parseLong(sc.nextLine());
                        vm.delete(delId);
                        System.out.println("삭제됨");
                        break;

                    case "s":
                        System.out.print("통계를 볼 목표 ID: ");
                        long sid = Long.parseLong(sc.nextLine());
                        GoalViewModel.GoalStats stats = vm.stats(sid, LocalDate.now());
                        System.out.println("[통계]");
                        System.out.println(" 최근 7일 완료 횟수 : " + stats.last7);
                        System.out.println(" 최근 30일 완료 횟수: " + stats.last30);
                        System.out.println(" 연속 달성 일수     : " + stats.streak);
                        break;

                    case "today":
                        LocalDate today = LocalDate.now();
                        List<Goal> all = vm.list();
                        System.out.println("[오늘(" + today + ") 목표 진행 상황]");
                        if (all.isEmpty()) {
                            System.out.println(" (등록된 목표 없음)");
                        } else {
                            for (Goal hh : all) {
                                boolean done = vm.isCompletedOn(hh.id, today);
                                int streak = vm.streak(hh.id, today);
                                String status = done ? "완료" : "미완료";
                                System.out.println(String.format(Locale.ROOT,
                                        " [#%d] %s (cat=%s) - 오늘: %s, 연속 %d일",
                                        hh.id, hh.name, hh.category, status, streak));
                            }
                        }
                        break;

                    default:
                        System.out.println("?");
                }
            } catch (Exception e) {
                System.out.println("오류: " + e.getMessage());
            }
        }
    }

    // author: soyoung0419, expected: 전공 용어 사전 인터랙션 , developed: 추가/수정/삭제/검색/카테고리/정렬/퀴즈/CSV export
    static void handleGlossary(Scanner sc, GlossaryViewModel vm) {
        Random rnd = new Random();
        while (true) {
            System.out.println("========================================");
            System.out.println("             [전공 용어 사전]");
            System.out.println("========================================");
            System.out.println(" a : 용어 추가");
            System.out.println(" e : 용어 수정");
            System.out.println(" d : 용어 삭제");
            System.out.println(" s : 용어 검색");
            System.out.println(" c : 카테고리별 목록");
            System.out.println(" l : 전체 목록");
            System.out.println(" q : 랜덤 퀴즈 모드");
            System.out.println(" csv : CSV 파일로 내보내기");
            System.out.println(" b : 뒤로가기");
            System.out.println("----------------------------------------");
            System.out.print("> ");

            String cmd = sc.nextLine().trim();
            if (cmd.equals("0")) return;

            try {
                switch (cmd) {
                    case "a": { // 용어 추가
                        System.out.print("용어: ");
                        String term = sc.nextLine();
                        System.out.print("정의: ");
                        String def = sc.nextLine();
                        System.out.print("카테고리(예: OS/DS/CN/DB/AI): ");
                        String cat = sc.nextLine();
                        System.out.print("난이도(1~5): ");
                        int diff = Integer.parseInt(sc.nextLine().trim());
                        System.out.print("태그(쉼표로 구분, 예: deadlock,os,scheduling): ");
                        String tags = sc.nextLine();
                        GlossaryEntry e = vm.add(term, def, cat, diff, tags);
                        System.out.println("추가됨:");
                        System.out.println(e);
                        break;
                    }
                    case "e": { // 용어 수정
                        System.out.print("수정할 용어 ID: ");
                        long id = Long.parseLong(sc.nextLine());
                        System.out.print("새 용어(변경 없으면 Enter): ");
                        String nt = sc.nextLine();
                        if (nt.isBlank()) nt = null;
                        System.out.print("새 정의(변경 없으면 Enter): ");
                        String nd = sc.nextLine();
                        if (nd.isBlank()) nd = null;
                        System.out.print("새 카테고리(변경 없으면 Enter): ");
                        String nc = sc.nextLine();
                        if (nc.isBlank()) nc = null;
                        System.out.print("새 난이도(1~5, 변경 없으면 Enter): ");
                        String ndiffStr = sc.nextLine().trim();
                        Integer ndiff = null;
                        if (!ndiffStr.isBlank()) ndiff = Integer.parseInt(ndiffStr);
                        System.out.print("새 태그(쉼표, 변경 없으면 Enter): ");
                        String ntags = sc.nextLine();
                        if (ntags.isBlank()) ntags = null;

                        vm.update(id, nt, nd, nc, ndiff, ntags);
                        System.out.println("수정 완료");
                        break;
                    }
                    case "d": { // 삭제
                        System.out.print("삭제할 용어 ID: ");
                        long id = Long.parseLong(sc.nextLine());
                        System.out.print("정말 삭제하시겠습니까? (y/n): ");
                        String yn = sc.nextLine().trim().toLowerCase(Locale.ROOT);
                        if (yn.equals("y")) {
                            vm.delete(id);
                            System.out.println("삭제됨");
                        } else {
                            System.out.println("삭제 취소");
                        }
                        break;
                    }
                    case "s": { // 검색
                        System.out.print("검색 키워드(용어/정의): ");
                        String kw = sc.nextLine();
                        List<GlossaryEntry> list = vm.search(kw);
                        if (list.isEmpty()) {
                            System.out.println("검색 결과 없음.");
                        } else {
                            for (GlossaryEntry e : list) {
                                System.out.println("----------------------------------------");
                                System.out.println(e);
                            }
                        }
                        break;
                    }
                    case "c": { // 카테고리별 목록
                        System.out.print("카테고리 입력(예: OS/DS/CN/DB/AI): ");
                        String cat = sc.nextLine();
                        List<GlossaryEntry> list = vm.byCategory(cat);
                        if (list.isEmpty()) {
                            System.out.println("해당 카테고리의 용어가 없습니다.");
                        } else {
                            for (GlossaryEntry e : list) {
                                System.out.println("----------------------------------------");
                                System.out.println(e);
                            }
                        }
                        break;
                    }

                    case "l": { // 전체 목록(추가된 순서 그대로)
                        List<GlossaryEntry> list = vm.all();
                        if (list.isEmpty()) {
                            System.out.println("등록된 용어가 없습니다.");
                        } else {
                            for (GlossaryEntry e : list) {
                                System.out.println("----------------------------------------");
                                System.out.println(e);
                            }
                        }
                        break;
                    }

                    case "q": { // 랜덤 퀴즈 모드
                        List<GlossaryEntry> all = vm.all();
                        if (all.isEmpty()) {
                            System.out.println("퀴즈를 출제할 용어가 없습니다. 먼저 몇 개 등록하세요.");
                            break;
                        }
                        System.out.println("퀴즈 타입 선택:");
                        System.out.println(" 1: 정의 → 용어 맞추기");
                        System.out.println(" 2: 용어 → 정의 맞추기(객관식)");
                        System.out.print("> ");
                        String qt = sc.nextLine().trim();
                        if (qt.equals("1")) {
                            // 정의 -> 용어
                            GlossaryEntry q = vm.randomOne(rnd);
                            if (q == null) {
                                System.out.println("용어가 부족합니다.");
                                break;
                            }
                            System.out.println("정의:");
                            System.out.println(q.definition);
                            System.out.print("이 정의에 해당하는 용어는? ");
                            String ans = sc.nextLine().trim();
                            if (ans.equalsIgnoreCase(q.term)) {
                                System.out.println("정답!");
                            } else {
                                System.out.println("오답. 정답: " + q.term);
                            }
                        } else if (qt.equals("2")) {
                            // 객관식: 용어 -> 정의
                            GlossaryEntry answer = vm.randomOne(rnd);
                            if (answer == null) {
                                System.out.println("용어가 부족합니다.");
                                break;
                            }
                            List<GlossaryEntry> opts = vm.randomOptions(rnd, answer, 4);
                            System.out.println("용어: " + answer.term);
                            for (int i = 0; i < opts.size(); i++) {
                                System.out.println((i + 1) + ") " + opts.get(i).definition);
                            }
                            System.out.print("정답 번호: ");
                            String choiceStr = sc.nextLine().trim();
                            try {
                                int ch = Integer.parseInt(choiceStr);
                                if (ch >= 1 && ch <= opts.size()
                                        && opts.get(ch - 1).id == answer.id) {
                                    System.out.println("정답!");
                                } else {
                                    System.out.println("오답. 정답 정의:");
                                    System.out.println(answer.definition);
                                }
                            } catch (NumberFormatException ex) {
                                System.out.println("숫자를 입력하세요.");
                            }
                        } else {
                            System.out.println("알 수 없는 퀴즈 타입입니다.");
                        }
                        break;
                    }
                    case "csv": { // CSV export
                        System.out.print("CSV 파일 이름(비우면 glossary_export.csv): ");
                        String fn = sc.nextLine().trim();
                        if (fn.isBlank()) fn = "glossary_export.csv";
                        vm.exportCsv(fn);
                        break;
                    }

                    // BACK
                    case "b":
                        return;
                        
                    default:
                        System.out.println("? (알 수 없는 명령)");
                }
            } catch (Exception e) {
                System.out.println("오류: " + e.getMessage());
            }
        }
    }



    // Seed some data for first run only
    static void seed(TaskViewModel taskVM, ScheduleViewModel schedVM, FriendViewModel friendVM, CalendarViewModel calVM){
        // author: soyoung0419, expected: 테스트용 친구 시드 , developed: 2명 기본 추가
        friendVM.add("민수", "uid_123");
        friendVM.add("지은", "uid_456");
        // author: soyoung0419, expected: 테스트용 일기 시드 , developed: 오늘 일기 1건
        calVM.addDiary(LocalDate.now(), "중간고사 대비", null);
    }
}
