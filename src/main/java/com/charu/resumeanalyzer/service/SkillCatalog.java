package com.charu.resumeanalyzer.service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;

/**
 * The vocabulary the analyzer knows about: a curated set of technical skills,
 * grouped by category, each with the aliases people actually type on resumes.
 */
@Component
public class SkillCatalog {

    private final Map<String, List<String>> categories = new LinkedHashMap<>();
    private final Map<String, Pattern> patterns = new LinkedHashMap<>();

    public SkillCatalog() {
        category("Languages",
                skill("Java", "java", "java 8", "java 11", "java 17", "core java"),
                skill("Kotlin", "kotlin"),
                skill("Python", "python", "python3"),
                skill("JavaScript", "javascript", "js", "es6"),
                skill("TypeScript", "typescript", "ts"),
                skill("C++", "c\\+\\+", "cpp"),
                skill("C#", "c#", "c sharp"),
                skill("Go", "golang", "go lang"),
                skill("Ruby", "ruby"),
                skill("PHP", "php"),
                skill("Scala", "scala"),
                skill("Rust", "rust"),
                skill("SQL", "sql"),
                skill("Shell", "bash", "shell scripting", "shell"));

        category("Backend",
                skill("Spring Boot", "spring boot", "springboot"),
                skill("Spring MVC", "spring mvc"),
                skill("Spring Security", "spring security"),
                skill("Spring Data JPA", "spring data jpa", "spring data"),
                skill("Hibernate", "hibernate"),
                skill("JPA", "jpa"),
                skill("REST APIs", "rest api", "restful", "rest apis", "rest"),
                skill("GraphQL", "graphql"),
                skill("gRPC", "grpc"),
                skill("Microservices", "microservice", "microservices"),
                skill("Node.js", "node\\.js", "nodejs", "node js"),
                skill("Express", "express\\.js", "expressjs", "express"),
                skill("Django", "django"),
                skill("Flask", "flask"),
                skill(".NET", "\\.net", "dotnet", "asp\\.net"));

        category("Frontend",
                skill("React", "react\\.js", "reactjs", "react"),
                skill("Angular", "angular"),
                skill("Vue", "vue\\.js", "vuejs", "vue"),
                skill("HTML", "html5", "html"),
                skill("CSS", "css3", "css"),
                skill("Tailwind", "tailwind"),
                skill("Bootstrap", "bootstrap"),
                skill("Redux", "redux"),
                skill("Next.js", "next\\.js", "nextjs"),
                skill("Thymeleaf", "thymeleaf"));

        category("Data",
                skill("MySQL", "mysql"),
                skill("PostgreSQL", "postgresql", "postgres"),
                skill("MongoDB", "mongodb", "mongo"),
                skill("Redis", "redis"),
                skill("Oracle", "oracle db", "oracle database", "oracle"),
                skill("Elasticsearch", "elasticsearch", "elastic search"),
                skill("Cassandra", "cassandra"),
                skill("Kafka", "kafka"),
                skill("RabbitMQ", "rabbitmq"),
                skill("Spark", "apache spark", "spark"),
                skill("Hadoop", "hadoop"),
                skill("ETL", "etl"),
                skill("Pandas", "pandas"),
                skill("NumPy", "numpy"));

        category("Cloud & DevOps",
                skill("AWS", "aws", "amazon web services"),
                skill("Azure", "azure"),
                skill("GCP", "gcp", "google cloud"),
                skill("Docker", "docker"),
                skill("Kubernetes", "kubernetes", "k8s"),
                skill("Jenkins", "jenkins"),
                skill("CI/CD", "ci/cd", "cicd", "continuous integration"),
                skill("Terraform", "terraform"),
                skill("Ansible", "ansible"),
                skill("Linux", "linux", "unix"),
                skill("Nginx", "nginx"),
                skill("GitHub Actions", "github actions"),
                skill("Prometheus", "prometheus"),
                skill("Grafana", "grafana"));

        category("Tools & Practices",
                skill("Git", "git", "github", "gitlab"),
                skill("Maven", "maven"),
                skill("Gradle", "gradle"),
                skill("JUnit", "junit"),
                skill("Mockito", "mockito"),
                skill("Selenium", "selenium"),
                skill("TDD", "tdd", "test driven development"),
                skill("Agile", "agile", "scrum"),
                skill("Jira", "jira"),
                skill("Swagger", "swagger", "openapi"),
                skill("Postman", "postman"),
                skill("Design Patterns", "design patterns"),
                skill("Data Structures", "data structures", "dsa"),
                skill("System Design", "system design"));

        category("AI & ML",
                skill("Machine Learning", "machine learning", "ml"),
                skill("Deep Learning", "deep learning"),
                skill("TensorFlow", "tensorflow"),
                skill("PyTorch", "pytorch"),
                skill("scikit-learn", "scikit-learn", "sklearn"),
                skill("NLP", "nlp", "natural language processing"),
                skill("LLM", "llm", "large language model"));
    }

    /** All known skill names, grouped by category. */
    public Map<String, List<String>> categories() {
        return categories;
    }

    /** Every skill name in the catalog. */
    public Set<String> allSkills() {
        Set<String> all = new LinkedHashSet<>();
        categories.values().forEach(all::addAll);
        return all;
    }

    /**
     * Finds every catalog skill mentioned in the given text, preserving catalog order
     * so results are stable between runs.
     */
    public Set<String> findIn(String text) {
        String haystack = text.toLowerCase(Locale.ROOT);
        Set<String> found = new LinkedHashSet<>();
        patterns.forEach((skillName, pattern) -> {
            if (pattern.matcher(haystack).find()) {
                found.add(skillName);
            }
        });
        return found;
    }

    private void category(String name, SkillDefinition... skills) {
        List<String> names = new ArrayList<>();
        for (SkillDefinition definition : skills) {
            names.add(definition.name());
            patterns.put(definition.name(), compile(definition.aliases()));
        }
        categories.put(name, names);
    }

    private SkillDefinition skill(String name, String... aliases) {
        return new SkillDefinition(name, List.of(aliases));
    }

    /**
     * Builds one alternation per skill. Aliases are already regex-escaped where they
     * contain metacharacters, and boundaries are hand-rolled because \b does not work
     * next to symbols like "+" or "#" (so "c++" would never match).
     */
    private Pattern compile(List<String> aliases) {
        String alternation = String.join("|", aliases);
        return Pattern.compile("(?<![a-z0-9+#.])(?:" + alternation + ")(?![a-z0-9+#])",
                Pattern.CASE_INSENSITIVE);
    }

    private record SkillDefinition(String name, List<String> aliases) {
    }
}
