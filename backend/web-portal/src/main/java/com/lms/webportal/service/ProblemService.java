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
                .completed(true)
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

        // Mô phỏng chấm code tự động cho đồ án Web
        ProblemDto problem = getProblemById(submitDto.getProblemId());
        String code = submitDto.getSourceCode() != null ? submitDto.getSourceCode().trim() : "";

        String status = "AC";
        int score = 100;
        int execTimeMs = 14;
        int memoryUsedKb = 2048;

        if (code.isBlank() || code.length() < 20) {
            status = "CE";
            score = 0;
        } else if (code.contains("error") || code.contains("throw") || code.contains("syntax_error")) {
            status = "WA";
            score = 40;
        }

        List<TestCaseResultDto> testCases = List.of(
                TestCaseResultDto.builder()
                        .testId(101L)
                        .status("AC".equals(status) ? "AC" : status)
                        .timeMs(12)
                        .memoryKb(1980)
                        .input("nums = [2,7,11,15], target = 9")
                        .expectedOutput("[0,1]")
                        .outputSnippet("[0,1]")
                        .hidden(false)
                        .build(),
                TestCaseResultDto.builder()
                        .testId(102L)
                        .status("AC".equals(status) ? "AC" : status)
                        .timeMs(14)
                        .memoryKb(2048)
                        .input("nums = [3,2,4], target = 6")
                        .expectedOutput("[1,2]")
                        .outputSnippet("[1,2]")
                        .hidden(false)
                        .build(),
                TestCaseResultDto.builder()
                        .testId(103L)
                        .status("AC".equals(status) ? "AC" : status)
                        .timeMs(15)
                        .memoryKb(2120)
                        .hidden(true)
                        .build()
        );

        String now = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        SubmissionResultDto result = SubmissionResultDto.builder()
                .submissionId("sub-" + UUID.randomUUID().toString().substring(0, 8))
                .problemId(submitDto.getProblemId())
                .userId(userId != null ? userId : "guest")
                .language(submitDto.getLanguage())
                .status(status)
                .score(score)
                .execTimeMs(execTimeMs)
                .memoryUsedKb(memoryUsedKb)
                .submittedAt(now)
                .sourceCode(submitDto.getSourceCode())
                .testCaseResults(testCases)
                .build();

        if (problem != null && "AC".equals(status)) {
            problem.setCompleted(true);
            problem.setTotalSubmit(problem.getTotalSubmit() + 1);
        }

        submissionHistory.add(0, result);
        return result;
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
