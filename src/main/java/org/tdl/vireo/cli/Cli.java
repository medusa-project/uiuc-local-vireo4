package org.tdl.vireo.cli;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.tdl.vireo.service.CliService;

/**
 * Activate the Vireo command line interface by passing the console argument to Maven
 *
 * mvn clean spring-boot:run -Dspring-boot.run.arguments="console"
 * 
 * NOTE: will enable allow submissions on institution
 * 
 * @author James Creel
 * @author Jeremy Huff
 */
@Order(value = Ordered.LOWEST_PRECEDENCE)
@Component
public class Cli implements CommandLineRunner {
    
    @Autowired
    CliService cliService;

    @Override
    public void run(String... args) throws Exception {
        boolean runConsole = false;
        for (String s : args) {
            if (s.equals("console")) {
                runConsole = true;
                break;
            }
        }

        Boolean running = runConsole ? true : false;

        if (running) {
            final String PROMPT = "\n" + (char) 27 + "[36mvireo>" + (char) 27 + "[37m ";

            Scanner reader = new Scanner(System.in); // Reading from System.in
            boolean expansive = false;
            int itemsGenerated = 0;

            System.out.print(PROMPT);

            while (running && reader.hasNextLine()) {

                String n = reader.nextLine();

                n = n.trim();

                String[] commandTokens = n.split("\\s+");
                List<String> commandArgs = new ArrayList<String>();

                String command = null;
                int num1 = 0;
                int num2 = 0;

                if (commandTokens.length > 0)
                    command = commandTokens[0];
                if (commandTokens.length > 1) {
                    for (int i = 1; i < commandTokens.length; i++) {
                        commandArgs.add(commandTokens[i]);
                    }

                }

                switch (command) {
                case "exit":
                    System.out.println("\nGoodbye.");
                    running = false;
                    break;

                case "accounts":
                    num1 = 0;
                    if (commandArgs.size() > 0) {
                        try {
                            num1 = Integer.parseInt(commandArgs.get(0));
                        } catch (Exception e) {
                            System.err.println("unable to parse as a number of items: " + commandArgs.get(0));
                        }
                    }

                    cliService.operateAccounts(expansive, num1);
                    break;

                case "admin_accounts":
                    num1 = 0;
                    if (commandArgs.size() > 0) {
                        try {
                            num1 = Integer.parseInt(commandArgs.get(0));
                        } catch (Exception e) {
                            System.err.println("unable to parse as a number of items: " + commandArgs.get(0));
                        }
                    }

                    cliService.operateAdminAccounts(expansive, num1);
                    break;

                case "expansive":
                    expansive = !expansive;
                    System.out.println("\nSet expansive = " + (expansive ? "true" : "false") + ".");
                    break;

                case "generate":
                    num1 = 0;
                    num2 = 100;
                    if (commandArgs.size() > 0) {
                        try {
                            // First argument is number of submissions.
                            num1 = Integer.parseInt(commandArgs.get(0));

                            // Second argument is maximum action logs.
                            if (commandArgs.size() > 1) {
                                num2 = Integer.parseInt(commandArgs.get(1));
                            }
                        } catch (Exception e) {
                            System.err.println("unable to parse as a number of items: " + commandArgs.get(0));
                        }
                    }

                    cliService.operateGenerate(expansive, num1, num2, itemsGenerated);
                    break;

                case "":
                    break;

                default:
                    System.out.println("Unknown command " + n);
                }

                if (running)
                    System.out.print(PROMPT);

            }
            reader.close();

        }
    }

}
