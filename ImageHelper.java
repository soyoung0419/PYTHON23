import java.io.*;
import java.nio.file.*;
import java.util.UUID;

public class ImageHelper {

    private static final String IMAGE_DIR = "images";

    // ────────────────────────────────
    // 디렉터리 보장
    // ────────────────────────────────
    private static File ensureDir() {
        File dir = new File(IMAGE_DIR);
        if (!dir.exists()) dir.mkdirs();
        return dir;
    }

    // ────────────────────────────────
    // 파일 확장자 추출
    // ────────────────────────────────
    private static String extractExtension(String name) {
        if (name == null) return "";
        int idx = name.lastIndexOf(".");
        return (idx == -1) ? "" : name.substring(idx);
    }

    // ────────────────────────────────
    // 랜덤 파일 이름 생성
    // ────────────────────────────────
    private static String randomFileName(String original) {
        String ext = extractExtension(original);
        return UUID.randomUUID().toString() + ext;
    }

    // ────────────────────────────────
    // 이미지 파일 저장 (복사)
    // ────────────────────────────────
    public static String saveImage(String srcPath) throws IOException {

        if (srcPath == null || srcPath.isBlank()) return null;

        File src = new File(srcPath);
        if (!src.exists())
            throw new FileNotFoundException("이미지 소스 파일 없음: " + srcPath);

        File dir = ensureDir();
        String newName = randomFileName(src.getName());
        File dst = new File(dir, newName);

        // 복사
        Files.copy(src.toPath(), dst.toPath(), StandardCopyOption.REPLACE_EXISTING);

        return IMAGE_DIR + "/" + newName;
    }

    // ────────────────────────────────
    // 이미지 열기
    // ────────────────────────────────
    public static void openImage(String path) {

        if (path == null || path.isBlank()) {
            System.out.println("이미지가 없습니다.");
            return;
        }

        File f = new File(path);
        if (!f.exists()) {
            System.out.println("이미지 파일 없음: " + path);
            return;
        }

        try {
            String os = System.getProperty("os.name").toLowerCase();
            Process p;

            if (os.contains("win")) {
                p = Runtime.getRuntime().exec(new String[]{"cmd", "/c", "start", "\"\"", f.getAbsolutePath()});
            } else if (os.contains("mac")) {
                p = Runtime.getRuntime().exec(new String[]{"open", f.getAbsolutePath()});
            } else {
                p = Runtime.getRuntime().exec(new String[]{"xdg-open", f.getAbsolutePath()});
            }

            p.waitFor();

        } catch (Exception e) {
            System.out.println("이미지 여는 중 오류: " + e.getMessage());
        }
    }

    // ────────────────────────────────
    // 이미지 삭제
    // ────────────────────────────────
    public static void deleteImage(String path) {

        if (path == null || path.isBlank()) return;

        File f = new File(path);
        if (!f.exists()) return;

        try {
            boolean ok = f.delete();
            if (!ok) {
                System.out.println("이미지 삭제 실패: " + path);
            }
        } catch (Exception e) {
            System.out.println("이미지 삭제 중 오류: " + e.getMessage());
        }
    }
}
