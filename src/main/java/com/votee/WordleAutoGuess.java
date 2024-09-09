package com.votee;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.votee.entity.CharacterGuess;
import com.votee.entity.GuessResult;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.*;

public class WordleAutoGuess {

    public String guessWord(int seed) {
        //PSEUDOCODE
        // 1. Initiate the candidates via loading from the file
        // 2. Choose the first guess
        // 3. Make a guess and get the feedback by calling the Votee API
        // 4. Filter the candidates basing on the feedback
        // 5. Choose the next guess(a optimal guess)
        // 5.1. To get the optimal guess, we use the scoring algorithm based-on frequency of the letters.
        // 6. Repeat step 3-5 until the word is guessed

        List<String> candidates = readCandidates();
        if (candidates.isEmpty()) {
            System.out.println("Can not load candidates");
            return "";
        }

        String guess = "slate";  // this is the first best guess in Wordle game
        int countStep = 0;
        while (true) {
            List<CharacterGuess> feedback = makeGuess(guess, seed);
            if (feedback.stream().allMatch(item -> item.result() == GuessResult.correct)) {
                System.out.printf("Total steps: %s%n", countStep);
                return guess;
            }

            candidates = filterCandidates(candidates, guess, feedback);
            if(candidates.isEmpty()) {
                return "";
            }

            guess = chooseNextGuess(candidates);
            countStep++;
        }


    }

    private List<String> readCandidates() {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            return objectMapper.readValue(new File("data/answers.txt"), new TypeReference<List<String>>() {
            });
        } catch (IOException ex) {
            ex.printStackTrace();
            return List.of();
        }
    }

    private List<CharacterGuess> makeGuess(String guess, int seed) {
        String url = String.format("https://wordle.votee.dev:8000/random?guess=%s&seed=%d", guess, seed);
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .build();

        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            ObjectMapper objectMapper = new ObjectMapper();
            return objectMapper.readValue(response.body(), new TypeReference<List<CharacterGuess>>() {
            });
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
            return List.of();
        }
    }

    private List<String> filterCandidates(List<String> candidates, String guess, List<CharacterGuess> feedback) {
        List<String> filteredCandidates = new ArrayList<>();
        candidates.forEach(candidate -> {
            boolean isMatched = true;
            for (int i = 0; i < 5; i++) {
                char letter = guess.charAt(i);
                CharacterGuess feedbackAtIndex = feedback.get(i);

                if (feedbackAtIndex.result() == GuessResult.correct && letter != candidate.charAt(i)) {
                    isMatched = false;
                    break;
                }

                if (feedbackAtIndex.result() == GuessResult.present && (letter == candidate.charAt(i) || !candidate.contains(String.valueOf(letter)))) {
                    isMatched = false;
                    break;
                }

                if (feedbackAtIndex.result() == GuessResult.absent && candidate.contains(String.valueOf(letter))) {
                    isMatched = false;
                    break;
                }
            }

            if (isMatched) {
                filteredCandidates.add(candidate);
            }
        });

        return filteredCandidates;
    }

    /**
     * Basing on scoring algorithm, we will calculate the score for each word basing on frequent of letters.
     * The score of word is multiple of all score of letters which is calculated basing on the formula: 1 + (letterFreq - maxFreq) ^ 2
     * The best next guest is the word which has min score in list of candidates
     */
    private String chooseNextGuess(List<String> candidates) {
        Map<String, int[]> letterFreq = makeLetterFreq(candidates);
        Map<String, Double> scoreForCandidates = calculateScoreForCandidates(candidates, letterFreq);

        return candidates.stream()
                .min(Comparator.comparing(scoreForCandidates::get))
                .orElse("");
    }

    private Map<String, int[]> makeLetterFreq(List<String> candidates) {
        Map<String, int[]> letterFreq = new HashMap<>();
        String alphabet = "abcdefghijklmnopqrstuvwxyz";
        for (char letter : alphabet.toCharArray()) {
            int[] freq = new int[5];
            for (int i = 0; i < 5; i++) {
                for (String candidate : candidates) {
                    if (candidate.charAt(i) == letter) {
                        freq[i] += 1;
                    }
                }
            }
            letterFreq.put(String.valueOf(letter), freq);
        }

        return letterFreq;
    }

    /**
     * Formula: 1 + (letterFreq - maxFreq) ^ 2
     */
    private Map<String, Double> calculateScoreForCandidates(List<String> candidates, Map<String, int[]> letterFreq) {
        Map<String, Double> scoresOfCandidates = new HashMap<>();

        // Find maximum frequencies of each position
        int[] maxFreq = new int[5];
        for (String c : letterFreq.keySet()) {
            int[] freq = letterFreq.get(c);
            for (int i = 0; i < 5; i++) {
                if (maxFreq[i] < freq[i]) {
                    maxFreq[i] = freq[i];
                }
            }
        }

        // Calculate score for each for basing on the formula
        for (String candidate : candidates) {
            double score = 1.0;

            for (int i = 0; i < 5; i++) {
                int[] letterFreqAtIndexI = letterFreq.get(String.valueOf(candidate.charAt(i)));
                if (letterFreqAtIndexI != null) {
                    score *= 1 + Math.pow((letterFreqAtIndexI[i] - maxFreq[i]), 2);
                }
            }
            //Since some words will have the same scores, we will plus a random value from 0 to 1 for fairness
            score += Math.random();
            scoresOfCandidates.put(candidate, score);
        }

        return scoresOfCandidates;
    }
}
