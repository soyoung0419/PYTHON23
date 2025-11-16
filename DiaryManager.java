// File: DiaryManager.java
// Standalone Diary Feature Manager (extracted from CampusPlanner)

import java.io.*;
import java.nio.file.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class DiaryManager {

    /* ===================== UTILS ===================== */
    static class DateTimeUtils {
        static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        static LocalDate parseDate(String s) { return LocalDate.parse(s, DATE_FMT); }
        static boolean isSameDay(LocalDate a, LocalDate b){ return Objects.equals(a, b); }
    }

    /* ===================== MODEL ===================== */
    static class Diary implements Serializable {
        private static final long serialVersionUID = 1L;
        final long id;
        LocalDate date;
        String text;
        String imagePath; // optional

        Diary(long id, LocalDate date, String text, String imagePath) {
            this.id = id;
            this.date = date;
            this.text = text;
            this.imagePath = imagePath;
        }

        @Override
        public String toString() {
            String imgInfo = (imagePath != null && !imagePath.isBlank()) ? " (이미지 첨부)" : "";
            return "[#" + id + "] " + date + " | " + text + imgInfo;
        }

        @Override public boolean equals(Object o){ return o instanceof Diary && ((Diary)o).id == id; }
        @Override public int hashCode(){ return Long.hashCode(id); }
    }

    /* ===================== IMAGE HELPER ===================== */
    static class ImageHelper {

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

        static String saveImage(String srcPath) throws IOException {
            if (srcPath == null || srcPath.isBlank()) return null;
            File src = new File(srcPath);
            if (!src.exists()) throw new FileNotFoundException("이미지 파일이 없습니다.");

            File dir = ensureImageDir();
            String newName = randomName(src.getName());
            File dst = new File(dir, newName);

            Files.copy(src.toPath(), dst.toPath(), StandardCopyOption.REPLACE_EXISTING);
            return "images/" + newName;
        }

        static void deleteImage(String path){
            if (path == null || path.isBlank()) return;
            File f = new File(path);
            if (f.exists()) f.delete();
        }

        static void openImage(String path){
            if (path == null || path.isBlank()) {
                System.out.println("이미지 없음");
                return;
            }
            File f = new File(path);
            if (!f.exists()) {
                System.out.println("파일 찾을 수 없음: " + path);
                return;
            }
            try {
                String os = System.getProperty("os.name").toLowerCase();
                Process p;
                if (os.contains("win"))
                    p = Runtime.getRuntime().exec(new String[]{"cmd","/c","start","\"\"",path});
                else if (os.contains("mac"))
                    p = Runtime.getRuntime().exec(new String[]{"open",path});
                else
                    p = Runtime.getRuntime().exec(new String[]{"xdg-open",path});
                p.waitFor();
            } catch (Exception e){
                System.out.println("이미지 열기 오류: " + e.getMessage());
            }
        }
    }

    /* ===================== LOCAL FILE DB ===================== */
    static class LocalStore implements Serializable {
        private static final long serialVersionUID = 1L;
        List<Diary> diaries = new ArrayList<>();
        long diarySeq = 1;
    }

    static class LocalDatabaseManager {
        private static final String FILE = "diary_data.bin";
        private static final String TMP = "diary_data.tmp";

        synchronized LocalStore load() {
            File f = new File(FILE);
            if (!f.exists()) return new LocalStore();
            try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(f))) {
                Object obj = ois.readObject();
                if (obj instanceof LocalStore) return (LocalStore)obj;
            } catch (Exception e){
                System.err.println("불러오기 실패: " + e.getMessage());
            }
            return new LocalStore();
        }

        synchronized void save(LocalStore store){
            try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(TMP))) {
                oos.writeObject(store);
            } catch (Exception e){
                System.err.println("저장 오류: " + e.getMessage());
                return;
            }
            try {
                Files.move(Paths.get(TMP), Paths.get(FILE),
                        StandardCopyOption.REPLACE_EXISTING,
                        StandardCopyOption.ATOMIC_MOVE);
            } catch(Exception e){
                System.err.println("교체 오류: " + e.getMessage());
            }
        }
    }

    /* ===================== DAO ===================== */
    static class DiaryDao {
        private final LocalStore store;
        private final LocalDatabaseManager db;

        DiaryDao(LocalStore store, LocalDatabaseManager db){
            this.store = store;
            this.db = db;
        }

        Diary addNew(LocalDate date, String text, String imagePath) {
            try {
                imagePath = ImageHelper.saveImage(imagePath);
            } catch (Exception e){
                throw new RuntimeException("이미지 저장 실패: " + e.getMessage());
            }

            Diary d = new Diary(store.diarySeq++, date, text, imagePath);
            store.diaries.add(d);
            db.save(store);
            return d;
        }

        Optional<Diary> get(long id){
            return store.diaries.stream().filter(d -> d.id == id).findFirst();
        }

        List<Diary> getAll(){
            return new ArrayList<>(store.diaries);
        }

        void delete(long id){
            get(id).ifPresent(d -> {
                ImageHelper.deleteImage(d.imagePath);
                store.diaries.remove(d);
                db.save(store);
            });
        }

        void update(Diary d){
            get(d.id).ifPresent(old -> {

                old.date = d.date;
                old.text = d.text;

                if ("__DELETE_IMAGE__".equals(d.imagePath)) {
                    ImageHelper.deleteImage(old.imagePath);
                    old.imagePath = null;
                }
                else if (d.imagePath != null && !d.imagePath.isBlank()) {
                    ImageHelper.deleteImage(old.imagePath);
                    try {
                        old.imagePath = ImageHelper.saveImage(d.imagePath);
                    } catch(Exception e){
                        throw new RuntimeException("이미지 저장 오류: " + e.getMessage());
                    }
                }

                db.save(store);
            });
        }

        List<Diary> byDate(LocalDate date){
            List<Diary> out = new ArrayList<>();
            for (Diary d : store.diaries)
                if (DateTimeUtils.isSameDay(d.date, date))
                    out.add(d);
            return out;
        }

        List<Diary> searchKeyword(String keyword){
            List<Diary> out = new ArrayList<>();
            for (Diary d : store.diaries)
                if (d.text != null && d.text.contains(keyword))
                    out.add(d);
            return out;
        }
    }

    /* ===================== VIEW MODEL ===================== */
    static class CalendarViewModel {
        private final DiaryDao diaries;

        CalendarViewModel(DiaryDao diaries){
            this.diaries = diaries;
        }

        Diary addDiary(LocalDate date, String text, String img){
            return diaries.addNew(date, text, img);
        }

        List<Diary> diariesOn(LocalDate date){
            return diaries.byDate(date);
        }

        List<Diary> getAllDiaries(){
            List<Diary> all = diaries.getAll();
            all.sort(Comparator.comparing(d -> d.date));
            return all;
        }

        List<Diary> searchKeyword(String keyword){
            return diaries.searchKeyword(keyword);
        }

        void updateDiary(long id, String newText, String newImg){
            diaries.get(id).ifPresent(d -> {
                if (newText != null && !newText.isBlank()) d.text = newText;

                if ("__DELETE_IMAGE__".equals(newImg)) d.imagePath = "__DELETE_IMAGE__";
                else if (newImg != null && !newImg.isBlank()) d.imagePath = newImg;

                diaries.update(d);
            });
        }

        void deleteDiary(long id){
            diaries.delete(id);
        }
    }

    /* ===================== CONSOLE UI ===================== */
    static void handleDiary(Scanner sc, CalendarViewModel vm){
        while(true){
            System.out.println("[다이어리] a.추가 l.날짜조회 all.전체보기 d.삭제 u.수정 img.보기 imgdel.이미지삭제 k.검색 b.뒤로");
            System.out.print("> ");
            String c = sc.nextLine().trim();
            if (c.equals("b")) return;

            switch (c){
                case "a":
                    System.out.print("날짜(yyyy-MM-dd): ");
                    LocalDate date = DateTimeUtils.parseDate(sc.nextLine());
                    System.out.print("내용: ");
                    String text = sc.nextLine();
                    System.out.print("이미지 경로(선택): ");
                    String img = sc.nextLine();
                    Diary added = vm.addDiary(date, text, img.isBlank()? null: img);
                    System.out.println("추가됨: " + added);
                    break;

                case "all":
                    vm.getAllDiaries().forEach(System.out::println);
                    break;

                case "l":
                    System.out.print("날짜(yyyy-MM-dd): ");
                    LocalDate d = DateTimeUtils.parseDate(sc.nextLine());
                    vm.diariesOn(d).forEach(System.out::println);
                    break;

                case "d":
                    System.out.print("삭제할 ID: ");
                    vm.deleteDiary(Long.parseLong(sc.nextLine()));
                    System.out.println("삭제됨");
                    break;

                case "u":
                    System.out.print("수정할 ID: ");
                    long id = Long.parseLong(sc.nextLine());
                    System.out.print("새 내용(Enter=유지): ");
                    String newText = sc.nextLine();
                    System.out.print("새 이미지 경로(Enter=유지): ");
                    String newImg = sc.nextLine();
                    vm.updateDiary(id, newText, newImg.isBlank()? null: newImg);
                    System.out.println("수정 완료");
                    break;

                case "img":
                    System.out.print("이미지 볼 ID: ");
                    long imgId = Long.parseLong(sc.nextLine());
                    vm.getAllDiaries().stream()
                            .filter(x -> x.id == imgId)
                            .findFirst()
                            .ifPresentOrElse(
                                di -> ImageHelper.openImage(di.imagePath),
                                () -> System.out.println("일기 없음")
                            );
                    break;

                case "imgdel":
                    System.out.print("이미지 삭제할 ID: ");
                    long delId = Long.parseLong(sc.nextLine());
                    vm.updateDiary(delId, null, "__DELETE_IMAGE__");
                    System.out.println("이미지 삭제됨");
                    break;

                case "k":
                    System.out.print("키워드: ");
                    String key = sc.nextLine();
                    vm.searchKeyword(key).forEach(System.out::println);
                    break;

                default: System.out.println("? 명령");
            }
        }
    }

    /* ===================== MAIN ===================== */
    public static void main(String[] args) {
        System.out.println("=== DiaryManager (Standalone) ===");

        LocalDatabaseManager db = new LocalDatabaseManager();
        LocalStore store = db.load();
        DiaryDao diaryDao = new DiaryDao(store, db);
        CalendarViewModel vm = new CalendarViewModel(diaryDao);

        try (Scanner sc = new Scanner(System.in)){
            while(true){
                System.out.println("\n1.다이어리  0.종료");
                System.out.print("> ");
                String c = sc.nextLine().trim();

                if (c.equals("0")) break;
                else if (c.equals("1")) handleDiary(sc, vm);
                else System.out.println("?");
            }
        }

        db.save(store);
        System.out.println("종료됨 (저장 완료)");
    }
}
