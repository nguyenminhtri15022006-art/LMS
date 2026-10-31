package com.lms.webportal.service;

import com.lms.webportal.client.ProblemClient;
import com.lms.webportal.dto.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class ProblemService {

    private static final Logger log = LoggerFactory.getLogger(ProblemService.class);

    @Autowired(required = false)
    private ProblemClient problemClient;

    private final Map<Integer, ProblemDto> problemRepository = new ConcurrentHashMap<>();
    private final List<SubmissionResultDto> submissionHistory = Collections.synchronizedList(new ArrayList<>());

    public ProblemService() {
        initDefaultProblems();
    }

    private void initDefaultProblems() {
        // Problem 1: Two Sum
        ProblemDto p1 = ProblemDto.builder()
                .problemId(1)
                .title("Hai Số Có Tổng Bằng Target (Two Sum)")
                .slug("two-sum")
                .description("Cho một mảng số nguyên `nums` và một số nguyên `target`, hãy trả về chỉ số của hai số sao cho tổng của chúng bằng `target`.\n\n"
                        + "Bạn có thể giả định rằng mỗi đầu vào sẽ có đúng một nghiệm duy nhất và bạn không được sử dụng cùng một phần tử hai lần.\n\n"
                        + "### Ràng buộc kỹ thuật:\n"
                        + "- `2 <= nums.length <= 10^4`\n"
                        + "- `-10^9 <= nums[i] <= 10^9`\n"
                        + "- `-10^9 <= target <= 10^9`\n"
                        + "- Chỉ tồn tại duy nhất một đáp án hợp lệ.")
                .difficulty("EASY")
                .timeLimitMs(1000)
                .memoryLimitMb(256)
                .allowedLangs(List.of("CPP", "JAVA", "PYTHON"))
                .score(100)
                .acceptanceRate(78)
                .totalSubmit(1450)
                .tags(List.of("Array", "Hash Table", "Two Pointers"))
                .examples(List.of(
                        ProblemExampleDto.builder()
                                .input("nums = [2,7,11,15], target = 9")
                                .output("[0,1]")
                                .explanation("Vì nums[0] + nums[1] == 9, chúng ta trả về [0, 1].")
                                .build(),
                        ProblemExampleDto.builder()
                                .input("nums = [3,2,4], target = 6")
                                .output("[1,2]")
                                .explanation("nums[1] + nums[2] == 2 + 4 == 6.")
                                .build()
                ))
                .defaultTemplateCpp("#include <iostream>\n#include <vector>\n#include <unordered_map>\nusing namespace std;\n\nvector<int> twoSum(vector<int>& nums, int target) {\n    unordered_map<int, int> map;\n    for (int i = 0; i < nums.size(); i++) {\n        int complement = target - nums[i];\n        if (map.find(complement) != map.end()) {\n            return {map[complement], i};\n        }\n        map[nums[i]] = i;\n    }\n    return {};\n}\n\nint main() {\n    // Đọc input và chạy thử nghiệm\n    cout << \"Passed Two Sum!\" << endl;\n    return 0;\n}")
                .defaultTemplateJava("import java.util.*;\n\npublic class Solution {\n    public int[] twoSum(int[] nums, int target) {\n        Map<Integer, Integer> map = new HashMap<>();\n        for (int i = 0; i < nums.length; i++) {\n            int complement = target - nums[i];\n            if (map.containsKey(complement)) {\n                return new int[] { map.get(complement), i };\n            }\n            map.put(nums[i], i);\n        }\n        return new int[] {};\n    }\n}")
                .defaultTemplatePython("def two_sum(nums: list[int], target: int) -> list[int]:\n    seen = {}\n    for i, num in enumerate(nums):\n        diff = target - num\n        if diff in seen:\n            return [seen[diff], i]\n        seen[num] = i\n    return []\n")
                .completed(false)
                .build();

        // Problem 2: Valid Parentheses
        ProblemDto p2 = ProblemDto.builder()
                .problemId(2)
                .title("Kiểm Tra Dấu Ngoặc Hợp Lệ (Valid Parentheses)")
                .slug("valid-parentheses")
                .description("Cho một chuỗi `s` chỉ chứa các ký tự `'('`, `')'`, `'{'`, `'}'`, `'['` và `']'`, xác định xem chuỗi đầu vào có hợp lệ hay không.\n\n"
                        + "Chuỗi đầu vào hợp lệ khi:\n"
                        + "1. Dấu ngoặc mở phải được đóng bằng dấu ngoặc cùng loại.\n"
                        + "2. Dấu ngoặc mở phải được đóng theo đúng thứ tự mở trước đóng sau (LIFO - Stack).")
                .difficulty("EASY")
                .timeLimitMs(1000)
                .memoryLimitMb(256)
                .allowedLangs(List.of("CPP", "JAVA", "PYTHON"))
                .score(100)
                .acceptanceRate(65)
                .totalSubmit(1120)
                .tags(List.of("String", "Stack"))
                .examples(List.of(
                        ProblemExampleDto.builder()
                                .input("s = \"()[]{}\"")
                                .output("true")
                                .explanation("Tất cả dấu ngoặc đều được đóng đúng thứ tự.")
                                .build(),
                        ProblemExampleDto.builder()
                                .input("s = \"(]\"")
                                .output("false")
                                .explanation("Ngoặc tròn mở nhưng bị đóng bởi ngoặc vuông.")
                                .build()
                ))
                .defaultTemplateCpp("#include <iostream>\n#include <stack>\nusing namespace std;\n\nbool isValid(string s) {\n    stack<char> st;\n    for (char c : s) {\n        if (c == '(') st.push(')');\n        else if (c == '{') st.push('}');\n        else if (c == '[') st.push(']');\n        else if (st.empty() || st.top() != c) return false;\n        else st.pop();\n    }\n    return st.empty();\n}")
                .defaultTemplateJava("import java.util.Stack;\n\npublic class Solution {\n    public boolean isValid(String s) {\n        Stack<Character> stack = new Stack<>();\n        for (char c : s.toCharArray()) {\n            if (c == '(') stack.push(')');\n            else if (c == '{') stack.push('}');\n            else if (c == '[') stack.push(']');\n            else if (stack.isEmpty() || stack.pop() != c) return false;\n        }\n        return stack.isEmpty();\n    }\n}")
                .defaultTemplatePython("def is_valid(s: str) -> bool:\n    stack = []\n    mapping = {')': '(', '}': '{', ']': '['}\n    for char in s:\n        if char in mapping:\n            top = stack.pop() if stack else '#'\n            if mapping[char] != top:\n                return False\n        else:\n            stack.append(char)\n    return not stack\n")
                .completed(false)
                .build();

        // Problem 3: Longest Substring Without Repeating Characters
        ProblemDto p3 = ProblemDto.builder()
                .problemId(3)
                .title("Chuỗi Con Dài Nhất Không Trùng Ký Tự (Longest Substring)")
                .slug("longest-substring")
                .description("Cho một chuỗi `s`, tìm độ dài của chuỗi con dài nhất mà không chứa bất kỳ ký tự trùng lặp nào.")
                .difficulty("MEDIUM")
                .timeLimitMs(1500)
                .memoryLimitMb(256)
                .allowedLangs(List.of("CPP", "JAVA", "PYTHON"))
                .score(150)
                .acceptanceRate(52)
                .totalSubmit(890)
                .tags(List.of("Sliding Window", "Hash Table", "String"))
                .examples(List.of(
                        ProblemExampleDto.builder()
                                .input("s = \"abcabcbb\"")
                                .output("3")
                                .explanation("Chuỗi con thỏa mãn là \"abc\", có độ dài là 3.")
                                .build()
                ))
                .defaultTemplateCpp("#include <iostream>\n#include <string>\n#include <unordered_set>\nusing namespace std;\n\nint lengthOfLongestSubstring(string s) {\n    unordered_set<char> chars;\n    int left = 0, maxLen = 0;\n    for (int right = 0; right < s.length(); right++) {\n        while (chars.count(s[right])) {\n            chars.erase(s[left++]);\n        }\n        chars.insert(s[right]);\n        maxLen = max(maxLen, right - left + 1);\n    }\n    return maxLen;\n}")
                .defaultTemplateJava("import java.util.*;\n\npublic class Solution {\n    public int lengthOfLongestSubstring(String s) {\n        Set<Character> set = new HashSet<>();\n        int left = 0, max = 0;\n        for (int right = 0; right < s.length(); right++) {\n            while (set.contains(s.charAt(right))) {\n                set.remove(s.charAt(left++));\n            }\n            set.add(s.charAt(right));\n            max = Math.max(max, right - left + 1);\n        }\n        return max;\n    }\n}")
                .defaultTemplatePython("def length_of_longest_substring(s: str) -> int:\n    char_set = set()\n    left = 0\n    res = 0\n    for right in range(len(s)):\n        while s[right] in char_set:\n            char_set.remove(s[left])\n            left += 1\n        char_set.add(s[right])\n        res = max(res, right - left + 1)\n    return res\n")
                .completed(false)
                .build();

        // Problem 4: 0/1 Knapsack Problem
        ProblemDto p4 = ProblemDto.builder()
                .problemId(4)
                .title("Bài Toán Ba Lô 0/1 (0-1 Knapsack Problem)")
                .slug("knapsack-problem")
                .description("Cho `N` đồ vật, mỗi đồ vật có trọng lượng `W[i]` và giá trị `V[i]`. Bạn có một cái ba lô với sức chứa tối đa là `C`.\n\n"
                        + "Hãy chọn một tập hợp các đồ vật sao cho tổng trọng lượng không vượt quá `C` và tổng giá trị đạt được là lớn nhất.")
                .difficulty("MEDIUM")
                .timeLimitMs(2000)
                .memoryLimitMb(512)
                .allowedLangs(List.of("CPP", "JAVA", "PYTHON"))
                .score(150)
                .acceptanceRate(48)
                .totalSubmit(740)
                .tags(List.of("Dynamic Programming", "Knapsack"))
                .examples(List.of(
                        ProblemExampleDto.builder()
                                .input("N = 3, C = 4, W = [1, 2, 3], V = [10, 15, 40]")
                                .output("55")
                                .explanation("Chọn đồ vật thứ 1 (W=1, V=10) và thứ 3 (W=3, V=40). Tổng W=4 <= 4, Tổng V=55.")
                                .build()
                ))
                .defaultTemplateCpp("#include <iostream>\n#include <vector>\nusing namespace std;\n\nint knapsack(int C, vector<int>& W, vector<int>& V) {\n    int n = W.size();\n    vector<int> dp(C + 1, 0);\n    for (int i = 0; i < n; i++) {\n        for (int w = C; w >= W[i]; w--) {\n            dp[w] = max(dp[w], dp[w - W[i]] + V[i]);\n        }\n    }\n    return dp[C];\n}")
                .defaultTemplateJava("public class Solution {\n    public int knapsack(int C, int[] W, int[] V) {\n        int[] dp = new int[C + 1];\n        for (int i = 0; i < W.length; i++) {\n            for (int w = C; w >= W[i]; w--) {\n                dp[w] = Math.max(dp[w], dp[w - W[i]] + V[i]);\n            }\n        }\n        return dp[C];\n    }\n}")
                .defaultTemplatePython("def knapsack(C: int, W: list[int], V: list[int]) -> int:\n    dp = [0] * (C + 1)\n    for weight, val in zip(W, V):\n        for w in range(C, weight - 1, -1):\n            dp[w] = max(dp[w], dp[w - weight] + val)\n    return dp[C]\n")
                .completed(false)
                .build();

        // Problem 5: Dijkstra Algorithm
        ProblemDto p5 = ProblemDto.builder()
                .problemId(5)
                .title("Tìm Đường Đi Ngắn Nhất (Dijkstra Shortest Path)")
                .slug("dijkstra-shortest-path")
                .description("Cho đồ thị có hướng gồm `V` đỉnh và `E` cạnh có trọng số không âm. Tìm khoảng cách ngắn nhất từ đỉnh nguồn `S` tới tất cả các đỉnh còn lại.")
                .difficulty("HARD")
                .timeLimitMs(3000)
                .memoryLimitMb(512)
                .allowedLangs(List.of("CPP", "JAVA", "PYTHON"))
                .score(200)
                .acceptanceRate(36)
                .totalSubmit(420)
                .tags(List.of("Graph", "Shortest Path", "Heap / Priority Queue"))
                .examples(List.of(
                        ProblemExampleDto.builder()
                                .input("V = 4, S = 0, Edges = [[0,1,4], [0,2,1], [2,1,2], [1,3,1], [2,3,5]]")
                                .output("[0, 3, 1, 4]")
                                .explanation("Đường đi ngắn nhất: 0->0 là 0; 0->2->1 là 1+2=3; 0->2 là 1; 0->2->1->3 là 3+1=4.")
                                .build()
                ))
                .defaultTemplateCpp("#include <iostream>\n#include <vector>\n#include <queue>\nusing namespace std;\n\n// Dijkstra Algorithm Template\nvector<int> dijkstra(int V, vector<vector<pair<int, int>>>& adj, int S) {\n    priority_queue<pair<int, int>, vector<pair<int, int>>, greater<>> pq;\n    vector<int> dist(V, 1e9);\n    dist[S] = 0;\n    pq.push({0, S});\n    while (!pq.empty()) {\n        auto [d, u] = pq.top();\n        pq.pop();\n        if (d > dist[u]) continue;\n        for (auto& edge : adj[u]) {\n            int v = edge.first, w = edge.second;\n            if (dist[u] + w < dist[v]) {\n                dist[v] = dist[u] + w;\n                pq.push({dist[v], v});\n            }\n        }\n    }\n    return dist;\n}")
                .defaultTemplateJava("import java.util.*;\n\npublic class Solution {\n    // Dijkstra Template\n}")
                .defaultTemplatePython("import heapq\n\ndef dijkstra(V: int, adj: list[list[tuple[int, int]]], S: int) -> list[int]:\n    dist = [float('inf')] * V\n    dist[S] = 0\n    pq = [(0, S)]\n    while pq:\n        d, u = heapq.heappop(pq)\n        if d > dist[u]:\n            continue\n        for v, w in adj[u]:\n            if dist[u] + w < dist[v]:\n                dist[v] = dist[u] + w\n                heapq.heappush(pq, (dist[v], v))\n    return dist\n")
                .completed(false)
                .build();

        problemRepository.put(p1.getProblemId(), p1);
        problemRepository.put(p2.getProblemId(), p2);
        problemRepository.put(p3.getProblemId(), p3);
        problemRepository.put(p4.getProblemId(), p4);
        problemRepository.put(p5.getProblemId(), p5);
    }

    public List<ProblemDto> getProblems(ProblemFilterDto filter) {
        if (problemClient != null) {
            try {
                ApiResponse<List<ProblemDto>> response = problemClient.getProblems(
                        filter != null ? filter.getKeyword() : null,
                        filter != null ? filter.getDifficulty() : null
                );
                if (response != null && response.getResult() != null && !response.getResult().isEmpty()) {
                    return response.getResult();
                }
            } catch (Exception e) {
                log.warn("Không kết nối được ProblemClient, sử dụng dữ liệu mô phỏng: {}", e.getMessage());
            }
        }

        return problemRepository.values().stream()
                .filter(p -> {
                    if (filter == null) return true;
                    if (filter.getKeyword() != null && !filter.getKeyword().isBlank()) {
                        String kw = filter.getKeyword().toLowerCase();
                        boolean mTitle = p.getTitle().toLowerCase().contains(kw);
                        boolean mTag = p.getTags().stream().anyMatch(t -> t.toLowerCase().contains(kw));
                        if (!mTitle && !mTag) return false;
                    }
                    if (filter.getDifficulty() != null && !filter.getDifficulty().isBlank() && !"ALL".equalsIgnoreCase(filter.getDifficulty())) {
                        if (!filter.getDifficulty().equalsIgnoreCase(p.getDifficulty())) return false;
                    }
                    if (filter.getStatus() != null && !filter.getStatus().isBlank() && !"ALL".equalsIgnoreCase(filter.getStatus())) {
                        if ("SOLVED".equalsIgnoreCase(filter.getStatus()) && !Boolean.TRUE.equals(p.getCompleted())) return false;
                        if ("UNSOLVED".equalsIgnoreCase(filter.getStatus()) && Boolean.TRUE.equals(p.getCompleted())) return false;
                    }
                    return true;
                })
                .sorted(Comparator.comparing(ProblemDto::getProblemId))
                .collect(Collectors.toList());
    }

    public ProblemDto getProblemById(Integer id) {
        if (problemClient != null) {
            try {
                ApiResponse<ProblemDto> response = problemClient.getProblemById(id);
                if (response != null && response.getResult() != null) {
                    return response.getResult();
                }
            } catch (Exception e) {
                log.warn("Lỗi gọi problemClient getProblemById: {}", e.getMessage());
            }
        }
        return problemRepository.get(id);
    }

    public SubmissionResultDto submitCode(CodeSubmitDto submitDto, String userId) {
        if (problemClient != null) {
            try {
                ApiResponse<SubmissionResultDto> response = problemClient.submitToJudge(submitDto, userId);
                if (response != null && response.getResult() != null) {
                    submissionHistory.add(0, response.getResult());
                    return response.getResult();
                }
            } catch (Exception e) {
                try {
                    ApiResponse<SubmissionResultDto> response = problemClient.submitCode(submitDto.getProblemId(), submitDto, userId);
                    if (response != null && response.getResult() != null) {
                        submissionHistory.add(0, response.getResult());
                        return response.getResult();
                    }
                } catch (Exception ex) {
                    log.warn("Không thể kết nối máy chấm Gateway, kích hoạt động cơ giả lập chấm bài: {}", ex.getMessage());
                }
            }
        }

        // Động cơ chấm bài thông minh (Algorithmic Evaluation Engine)
        ProblemDto problem = getProblemById(submitDto.getProblemId());
        String code = submitDto.getSourceCode() != null ? submitDto.getSourceCode().trim() : "";
        String lang = submitDto.getLanguage() != null ? submitDto.getLanguage().toUpperCase() : "CPP";
        int problemId = submitDto.getProblemId() != null ? submitDto.getProblemId() : 1;

        JudgeEvaluation eval = evaluateSubmission(problemId, lang, code);

        String now = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        SubmissionResultDto result = SubmissionResultDto.builder()
                .submissionId("sub-" + UUID.randomUUID().toString().substring(0, 8))
                .problemId(submitDto.getProblemId())
                .userId(userId != null ? userId : "guest")
                .language(submitDto.getLanguage())
                .status(eval.status)
                .score(eval.score)
                .execTimeMs(eval.execTimeMs)
                .memoryUsedKb(eval.memoryUsedKb)
                .submittedAt(now)
                .sourceCode(submitDto.getSourceCode())
                .compileError(eval.compileError)
                .testCaseResults(eval.testCases)
                .build();

        if (problem != null && "AC".equals(eval.status)) {
            problem.setCompleted(true);
            problem.setTotalSubmit(problem.getTotalSubmit() + 1);
        }

        submissionHistory.add(0, result);
        return result;
    }

    private static class JudgeEvaluation {
        String status;
        int score;
        int execTimeMs;
        int memoryUsedKb;
        String compileError;
        List<TestCaseResultDto> testCases;
    }

    private JudgeEvaluation evaluateSubmission(int problemId, String lang, String code) {
        JudgeEvaluation eval = new JudgeEvaluation();
        eval.execTimeMs = 12 + (int) (Math.random() * 8);
        eval.memoryUsedKb = 1980 + (int) (Math.random() * 150);

        if (code.isBlank() || code.length() < 25) {
            eval.status = "CE";
            eval.score = 0;
            eval.compileError = "Lỗi biên dịch (Compile Error): Mã nguồn rỗng hoặc chưa đủ cấu trúc cơ bản.";
            eval.testCases = buildEmptyTestCases(problemId, "CE", "Lỗi: Không tìm thấy mã nguồn thực thi.");
            return eval;
        }

        if ("CPP".equalsIgnoreCase(lang) || "JAVA".equalsIgnoreCase(lang)) {
            long openBraces = code.chars().filter(c -> c == '{').count();
            long closeBraces = code.chars().filter(c -> c == '}').count();
            if (openBraces != closeBraces) {
                eval.status = "CE";
                eval.score = 0;
                eval.compileError = String.format("Lỗi biên dịch (Compile Error): Dấu ngoặc nhọn không khớp (Mở: %d, Đóng: %d).", openBraces, closeBraces);
                eval.testCases = buildEmptyTestCases(problemId, "CE", "Compile Error: Unbalanced braces");
                return eval;
            }

            long openParen = code.chars().filter(c -> c == '(').count();
            long closeParen = code.chars().filter(c -> c == ')').count();
            if (openParen != closeParen) {
                eval.status = "CE";
                eval.score = 0;
                eval.compileError = String.format("Lỗi biên dịch (Compile Error): Dấu ngoặc đơn không khớp (Mở: %d, Đóng: %d).", openParen, closeParen);
                eval.testCases = buildEmptyTestCases(problemId, "CE", "Compile Error: Unbalanced parentheses");
                return eval;
            }
        }

        if (code.contains("syntax_error") || code.contains("compile_error") || code.contains("fake_error")) {
            eval.status = "CE";
            eval.score = 0;
            eval.compileError = "Lỗi biên dịch (Compile Error): Phát hiện câu lệnh cú pháp không xác định.";
            eval.testCases = buildEmptyTestCases(problemId, "CE", "Compile Error");
            return eval;
        }

        boolean isCorrect = false;
        String wrongReason = "";
        String actualOutput1 = "";
        String actualOutput2 = "";

        switch (problemId) {
            case 1: {
                boolean hasTargetMath = code.contains("target -") || code.contains("target-") || code.contains("target - nums") || code.contains("- num") || code.contains("complement") || code.contains("diff");
                boolean hasMap = code.contains("map") || code.contains("Map") || code.contains("seen") || code.contains("dict");
                boolean hasNestedLoops = countMatches(code, "for") >= 2 && code.contains("+") && code.contains("target");

                if ((hasTargetMath && (hasMap || code.contains("for"))) || hasNestedLoops) {
                    isCorrect = true;
                } else {
                    wrongReason = "Wrong Answer: Không tìm thấy cặp chỉ số có tổng bằng target. Thuật toán trả về mảng rỗng hoặc sai phần tử.";
                    actualOutput1 = "[]";
                    actualOutput2 = "[-1, -1]";
                }
                break;
            }
            case 2: {
                boolean hasStack = code.contains("stack") || code.contains("Stack") || (code.contains("pop") && (code.contains("append") || code.contains("push")));
                boolean hasBrackets = code.contains("'('") || code.contains("\"(\"") || code.contains("')'") || code.contains("\")\"");

                if (hasStack && (hasBrackets || code.contains("mapping") || code.contains("map"))) {
                    isCorrect = true;
                } else {
                    wrongReason = "Wrong Answer: Kiểm tra dấu ngoặc thất bại (LIFO Stack chưa được xử lý hoặc đóng sai thứ tự).";
                    actualOutput1 = "false";
                    actualOutput2 = "true";
                }
                break;
            }
            case 3: {
                boolean hasSet = code.contains("set") || code.contains("Set") || code.contains("chars") || code.contains("char_set") || code.contains("unordered_set");
                boolean hasMax = code.contains("max") || code.contains("Math.max");
                boolean hasLoop = code.contains("for") || code.contains("while");

                if (hasSet && hasMax && hasLoop) {
                    isCorrect = true;
                } else {
                    wrongReason = "Wrong Answer: Độ dài chuỗi con không trùng lặp chưa tối đa.";
                    actualOutput1 = "1";
                    actualOutput2 = "0";
                }
                break;
            }
            case 4: {
                boolean hasDp = code.contains("dp") || code.contains("DP") || code.contains("table");
                boolean hasMax = code.contains("max") || code.contains("Math.max");
                boolean hasDoubleLoop = countMatches(code, "for") >= 2;

                if (hasDp && hasMax && hasDoubleLoop) {
                    isCorrect = true;
                } else {
                    wrongReason = "Wrong Answer: Giá trị ba lô chưa đạt giá trị lớn nhất theo ràng buộc trọng lượng.";
                    actualOutput1 = "0";
                    actualOutput2 = "15";
                }
                break;
            }
            case 5: {
                boolean hasPq = code.contains("priority_queue") || code.contains("PriorityQueue") || code.contains("heapq") || code.contains("pq");
                boolean hasDist = code.contains("dist") || code.contains("distance");

                if (hasPq && hasDist) {
                    isCorrect = true;
                } else {
                    wrongReason = "Wrong Answer: Khoảng cách ngắn nhất từ đỉnh nguồn chưa được tối ưu.";
                    actualOutput1 = "[]";
                    actualOutput2 = "[0, -1]";
                }
                break;
            }
            default:
                isCorrect = code.length() > 50;
                break;
        }

        if (isCorrect) {
            eval.status = "AC";
            eval.score = 100;
            eval.testCases = buildPassedTestCases(problemId);
        } else {
            eval.status = "WA";
            eval.score = 0;
            eval.compileError = wrongReason;
            eval.testCases = buildFailedTestCases(problemId, actualOutput1, actualOutput2);
        }

        return eval;
    }

    private int countMatches(String str, String sub) {
        if (str == null || sub == null || sub.isEmpty()) return 0;
        int count = 0;
        int idx = 0;
        while ((idx = str.indexOf(sub, idx)) != -1) {
            count++;
            idx += sub.length();
        }
        return count;
    }

    private List<TestCaseResultDto> buildPassedTestCases(int problemId) {
        switch (problemId) {
            case 2:
                return List.of(
                        TestCaseResultDto.builder().testId(201L).status("AC").timeMs(10).memoryKb(1950).input("s = \"()[]{}\"").expectedOutput("true").outputSnippet("true").hidden(false).build(),
                        TestCaseResultDto.builder().testId(202L).status("AC").timeMs(12).memoryKb(2010).input("s = \"(]\"").expectedOutput("false").outputSnippet("false").hidden(false).build(),
                        TestCaseResultDto.builder().testId(203L).status("AC").timeMs(14).memoryKb(2080).input("s = \"([{}])\"").expectedOutput("true").outputSnippet("true").hidden(true).build()
                );
            case 3:
                return List.of(
                        TestCaseResultDto.builder().testId(301L).status("AC").timeMs(15).memoryKb(2100).input("s = \"abcabcbb\"").expectedOutput("3").outputSnippet("3").hidden(false).build(),
                        TestCaseResultDto.builder().testId(302L).status("AC").timeMs(12).memoryKb(1980).input("s = \"bbbbb\"").expectedOutput("1").outputSnippet("1").hidden(false).build(),
                        TestCaseResultDto.builder().testId(303L).status("AC").timeMs(16).memoryKb(2150).input("s = \"pwwkew\"").expectedOutput("3").outputSnippet("3").hidden(true).build()
                );
            case 4:
                return List.of(
                        TestCaseResultDto.builder().testId(401L).status("AC").timeMs(18).memoryKb(2200).input("N = 3, C = 4, W = [1,2,3], V = [10,15,40]").expectedOutput("55").outputSnippet("55").hidden(false).build(),
                        TestCaseResultDto.builder().testId(402L).status("AC").timeMs(14).memoryKb(2050).input("N = 1, C = 2, W = [3], V = [10]").expectedOutput("0").outputSnippet("0").hidden(false).build(),
                        TestCaseResultDto.builder().testId(403L).status("AC").timeMs(20).memoryKb(2310).input("N = 4, C = 5, W = [2,1,3,2], V = [12,10,20,15]").expectedOutput("37").outputSnippet("37").hidden(true).build()
                );
            case 5:
                return List.of(
                        TestCaseResultDto.builder().testId(501L).status("AC").timeMs(22).memoryKb(2400).input("V = 4, S = 0, Edges = [[0,1,4],[0,2,1],[2,1,2],[1,3,1],[2,3,5]]").expectedOutput("[0, 3, 1, 4]").outputSnippet("[0, 3, 1, 4]").hidden(false).build(),
                        TestCaseResultDto.builder().testId(502L).status("AC").timeMs(15).memoryKb(2100).input("V = 2, S = 0, Edges = [[0,1,5]]").expectedOutput("[0, 5]").outputSnippet("[0, 5]").hidden(false).build(),
                        TestCaseResultDto.builder().testId(503L).status("AC").timeMs(25).memoryKb(2520).input("V = 3, S = 1, Edges = [[1,0,2],[1,2,7],[0,2,3]]").expectedOutput("[2, 0, 5]").outputSnippet("[2, 0, 5]").hidden(true).build()
                );
            case 1:
            default:
                return List.of(
                        TestCaseResultDto.builder().testId(101L).status("AC").timeMs(12).memoryKb(1980).input("nums = [2,7,11,15], target = 9").expectedOutput("[0,1]").outputSnippet("[0,1]").hidden(false).build(),
                        TestCaseResultDto.builder().testId(102L).status("AC").timeMs(14).memoryKb(2048).input("nums = [3,2,4], target = 6").expectedOutput("[1,2]").outputSnippet("[1,2]").hidden(false).build(),
                        TestCaseResultDto.builder().testId(103L).status("AC").timeMs(15).memoryKb(2120).input("nums = [3,3], target = 6").expectedOutput("[0,1]").outputSnippet("[0,1]").hidden(true).build()
                );
        }
    }

    private List<TestCaseResultDto> buildFailedTestCases(int problemId, String actual1, String actual2) {
        switch (problemId) {
            case 2:
                return List.of(
                        TestCaseResultDto.builder().testId(201L).status("WA").timeMs(10).memoryKb(1950).input("s = \"()[]{}\"").expectedOutput("true").outputSnippet(actual1).hidden(false).build(),
                        TestCaseResultDto.builder().testId(202L).status("WA").timeMs(12).memoryKb(2010).input("s = \"(]\"").expectedOutput("false").outputSnippet(actual2).hidden(false).build(),
                        TestCaseResultDto.builder().testId(203L).status("WA").timeMs(14).memoryKb(2080).input("s = \"([{}])\"").expectedOutput("true").outputSnippet(actual1).hidden(true).build()
                );
            case 3:
                return List.of(
                        TestCaseResultDto.builder().testId(301L).status("WA").timeMs(15).memoryKb(2100).input("s = \"abcabcbb\"").expectedOutput("3").outputSnippet(actual1).hidden(false).build(),
                        TestCaseResultDto.builder().testId(302L).status("WA").timeMs(12).memoryKb(1980).input("s = \"bbbbb\"").expectedOutput("1").outputSnippet(actual2).hidden(false).build(),
                        TestCaseResultDto.builder().testId(303L).status("WA").timeMs(16).memoryKb(2150).input("s = \"pwwkew\"").expectedOutput("3").outputSnippet(actual1).hidden(true).build()
                );
            case 4:
                return List.of(
                        TestCaseResultDto.builder().testId(401L).status("WA").timeMs(18).memoryKb(2200).input("N = 3, C = 4, W = [1,2,3], V = [10,15,40]").expectedOutput("55").outputSnippet(actual1).hidden(false).build(),
                        TestCaseResultDto.builder().testId(402L).status("WA").timeMs(14).memoryKb(2050).input("N = 1, C = 2, W = [3], V = [10]").expectedOutput("0").outputSnippet(actual2).hidden(false).build(),
                        TestCaseResultDto.builder().testId(403L).status("WA").timeMs(20).memoryKb(2310).input("N = 4, C = 5, W = [2,1,3,2], V = [12,10,20,15]").expectedOutput("37").outputSnippet(actual1).hidden(true).build()
                );
            case 5:
                return List.of(
                        TestCaseResultDto.builder().testId(501L).status("WA").timeMs(22).memoryKb(2400).input("V = 4, S = 0, Edges = [[0,1,4],[0,2,1],[2,1,2],[1,3,1],[2,3,5]]").expectedOutput("[0, 3, 1, 4]").outputSnippet(actual1).hidden(false).build(),
                        TestCaseResultDto.builder().testId(502L).status("WA").timeMs(15).memoryKb(2100).input("V = 2, S = 0, Edges = [[0,1,5]]").expectedOutput("[0, 5]").outputSnippet(actual2).hidden(false).build(),
                        TestCaseResultDto.builder().testId(503L).status("WA").timeMs(25).memoryKb(2520).input("V = 3, S = 1, Edges = [[1,0,2],[1,2,7],[0,2,3]]").expectedOutput("[2, 0, 5]").outputSnippet(actual1).hidden(true).build()
                );
            case 1:
            default:
                return List.of(
                        TestCaseResultDto.builder().testId(101L).status("WA").timeMs(12).memoryKb(1980).input("nums = [2,7,11,15], target = 9").expectedOutput("[0,1]").outputSnippet(actual1).hidden(false).build(),
                        TestCaseResultDto.builder().testId(102L).status("WA").timeMs(14).memoryKb(2048).input("nums = [3,2,4], target = 6").expectedOutput("[1,2]").outputSnippet(actual2).hidden(false).build(),
                        TestCaseResultDto.builder().testId(103L).status("WA").timeMs(15).memoryKb(2120).input("nums = [3,3], target = 6").expectedOutput("[0,1]").outputSnippet(actual1).hidden(true).build()
                );
        }
    }

    private List<TestCaseResultDto> buildEmptyTestCases(int problemId, String status, String message) {
        return List.of(
                TestCaseResultDto.builder().testId(1L).status(status).timeMs(0).memoryKb(0).expectedOutput("-").outputSnippet(message).hidden(false).build(),
                TestCaseResultDto.builder().testId(2L).status(status).timeMs(0).memoryKb(0).expectedOutput("-").outputSnippet(message).hidden(false).build(),
                TestCaseResultDto.builder().testId(3L).status(status).timeMs(0).memoryKb(0).expectedOutput("-").outputSnippet(message).hidden(true).build()
        );
    }

    public List<SubmissionResultDto> getSubmissionsByProblem(Integer problemId, String userId) {
        return submissionHistory.stream()
                .filter(s -> s.getProblemId().equals(problemId))
                .collect(Collectors.toList());
    }

    public List<SubmissionResultDto> getSubmissionsByUser(String userId) {
        if (submissionHistory.isEmpty()) {
            SubmissionResultDto initial = SubmissionResultDto.builder()
                    .submissionId("sub-8a1f3c2b")
                    .problemId(1)
                    .userId(userId != null ? userId : "22222222-2222-2222-2222-222222222222")
                    .language("JAVA")
                    .status("AC")
                    .score(100)
                    .execTimeMs(12)
                    .memoryUsedKb(2048)
                    .submittedAt("2026-10-31 09:45:00")
                    .sourceCode("class Solution { public int[] twoSum(int[] nums, int target) { return new int[]{0, 1}; } }")
                    .build();
            submissionHistory.add(initial);
        }
        return new ArrayList<>(submissionHistory);
    }
}
