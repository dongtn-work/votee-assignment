package com.votee;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class Main {
    public static void main(String[] args) throws IOException {
        WordleAutoGuess wordleAutoGuess = new WordleAutoGuess();

        while(true){
            System.out.print("Please providing the seed number: ");
            BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
            String command = reader.readLine();
            if(command.compareToIgnoreCase("exit") == 0){
                break;
            }

            if(isNumeric(command)) {
                String foundedWord = wordleAutoGuess.guessWord(Integer.parseInt(command));
                if(foundedWord.isEmpty()) {
                    System.out.println("Not found word");
                } else {
                    System.out.printf("Founded word: %s%n", foundedWord);
                }
            }
            System.out.println();
        }
    }

    public static boolean isNumeric(String input) {
        if(input == null || input.isEmpty()){
            return false;
        }
        try{
            Integer.parseInt(input);
            return true;
        }catch (NumberFormatException ex){
            return false;
        }
    }
}