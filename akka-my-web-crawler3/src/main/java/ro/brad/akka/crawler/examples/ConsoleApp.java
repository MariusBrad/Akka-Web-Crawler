/**
 * 
 */
package ro.brad.akka.crawler.examples;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import ro.brad.akka.crawler.actor.MasterActor;
import ro.brad.akka.crawler.actor.ActorMessages.CrawlSite;
import ro.brad.akka.crawler.actor.ActorMessages.CrawlNewsSite;
import ro.brad.akka.crawler.model.Globals;
import ro.brad.akka.crawler.model.Globals.CrawlingType;
import ro.brad.akka.crawler.model.Globals.IndexOpenMode;
import ro.brad.akka.crawler.model.Globals.NewsAgencies;

/**
 * @author marius
 *
 */
public class ConsoleApp {
	private final ActorSystem actorSystem;
	private ActorRef master;
	// private final Logger log = LoggerFactory.getLogger(App.class);
	private final Logger log = Logger.getLogger("App");

	/**
	 * @param args
	 */
	public static void main(String[] args) {

		String domain = null;
		boolean update = false;
		int hitsCount = 10;

		String usage = Globals.SIMPLE_USAGE + System.lineSeparator() + System.lineSeparator() + Globals.NEWS_USAGE;

		if (args[0].equals(Globals.SIMPLE_EXEC)) {
			for (int i = 1; i < args.length; i++) {
				if (args[i].equals(Globals.DOMAIN_OPT)) {
					domain = args[i + 1];
					i++;
				} else if (args[i].equals(Globals.UPDATE_OPT)) {
					update = true;
				}
			}
			if (domain == null) {
				System.err.println("Incorrect domain!");
				System.err.println("Usage: " + usage);
				System.exit(1);
			}

			new ConsoleApp().crawlSimple(domain, update ? IndexOpenMode.APPEND : IndexOpenMode.CREATE);
		} else if (args[0].equals(Globals.NEWS_EXEC)) {
			for (int i = 1; i < args.length; i++) {
				if (args[i].equals(Globals.HITS_OPT)) {
					hitsCount = Integer.parseInt(args[i + 1]);
					i++;
				} else if (args[i].equals(Globals.UPDATE_OPT)) {
					update = true;
				}
			}

			new ConsoleApp().crawlNews(hitsCount, update ? IndexOpenMode.APPEND : IndexOpenMode.CREATE);
		} else {
			System.err.println("Usage: " + usage);
			System.exit(1);
		}
	}

	public ConsoleApp() {
		actorSystem = ActorSystem.create(Globals.ACTOR_SYSTEM);
		log.info("Actor System " + Globals.ACTOR_SYSTEM + " created");
	}

	public void crawlSimple(String domain, IndexOpenMode mode) {

		master = actorSystem.actorOf(MasterActor.props(mode, CrawlingType.SIMPLE), Globals.MASTER_ACTOR);
		System.out.println("Welcome to the " + Globals.SIMPLE_EXEC + " crawling!\n");
		master.tell(new CrawlSite(domain), actorSystem.guardian());
	}

	public void crawlNews(int hitsCount, IndexOpenMode mode) {
		master = actorSystem.actorOf(MasterActor.props(mode, CrawlingType.NEWS), Globals.MASTER_ACTOR);

		int optionSelected;
		boolean finished = false;
		int phase = 1;

		// Create a console interactive menu
		System.out.println("Welcome to the " + Globals.NEWS_EXEC + " crawling!");
		System.out.println("At the end you will get " + Integer.toString(hitsCount) + " results");

		while (!finished) {
			if (phase == 1) {
				printMenu1();
				optionSelected = inNumber("Enter option: ");
				switch (optionSelected) {
				case 1: // Tolo.ro
					crawlTolo();
					break;
				case 2: // Evz.ro
					crawlEvz();
					break;
				case 3: // Digi24.ro
					crawlDigi24();
					break;
				case 4:
					System.out.println("Terminated!");
					finished = true;
					break;

				default:
					break;
				}

				if (!finished) {
					phase = 2;
				}
			} else if (phase == 2) {
				printMenu2();
				optionSelected = inNumber("Enter option: ");
				switch (optionSelected) {
				case 1: // Tolo.ro
					crawlTolo();
					break;
				case 2: // Evz.ro
					crawlEvz();
					break;
				case 3: //Digi24.ro
					crawlDigi24();
					break;
				case 4:
					actorSystem.terminate();
					break;

				default:
					break;
				}
			}
		}
	}

	protected void printMenu1() {
		System.out.println("|   	MENU Choose news agency   	|");
		System.out.println("| 	Options:                      	|");
		System.out.println("|        	1. Tolo.ro            	|");
		System.out.println("|        	2. Evz.ro             	|");
		System.out.println("|        	3. Digi24.ro          	|");
		System.out.println("|        	4. Exit        	  	  	|");
	}

	protected void printMenu2() {
		System.out.println("|   	MENU Choose news agency     |");
		System.out.println("| 	Options:                    	|");
		System.out.println("|        	1. Tolo.ro           	|");
		System.out.println("|        	2. Evz.ro            	|");
		System.out.println("|        	3. Digi24.ro         	|");
		System.out.println("|        	4. STOP indexing     	|");
	}

	protected void printMenu3() {
		System.out.println("|     TOLO Choose news categories   |");
		System.out.println("| Multiple choices.                 |");
		System.out.println("| Insert options separated by space.|");
		System.out.println("|        1. Football           		|");
		System.out.println("|        2. Sports            		|");
		System.out.println("|        3. Investigations         	|");
		System.out.println("|        4. Media     			    |");
		System.out.println("|        5. Miscellaneous  			|");
		System.out.println("|        6. Recommendations			|");
	}

	protected void printMenu4() {
		System.out.println("|     EVZ Choose news categories    |");
		System.out.println("| Multiple choices.                 |");
		System.out.println("| Insert options separated by space.|");
		System.out.println("|        1. Politics           		|");
		System.out.println("|        2. Economics            	|");
		System.out.println("|        3. Sports         			|");
		System.out.println("|        4. Culture     			|");
		System.out.println("|        5. International  			|");
		System.out.println("|        6. Social		  			|");
		System.out.println("|        7. Entertainment  			|");
	}

	protected void printMenu5() {
		System.out.println("|   DIGI24 Choose news categories   |");
		System.out.println("| Multiple choices.                 |");
		System.out.println("| Insert options separated by space.|");
		System.out.println("|        1. Politics           		|");
		System.out.println("|        2. Economics            	|");
		System.out.println("|        3. Sports         			|");
		System.out.println("|        4. Health     			    |");
		System.out.println("|        5. International  			|");
		System.out.println("|        6. Social		  			|");
	}

	// Prompt the user with messages
	protected void prompt(String prompt) {
		System.out.print(Globals.PROMPT_SIGN + prompt + " ");
		System.out.flush();
	}

	// Empty the stream before first use
	protected void inputFlush() {
		try {
			while ((System.in.available()) != 0) {
				System.in.read();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	// Process strings
	protected String inString() {
		int aChar;
		String s = "";
		boolean finished = false;

		while (!finished) {
			try {
				aChar = System.in.read();
				if (aChar < 0 || (char) aChar == '\n')
					finished = true;
				else if ((char) aChar != '\r')
					s = s + (char) aChar;
			}

			catch (java.io.IOException e) {
				System.out.println("Input error");
				finished = true;
			}
		}
		return s;
	}

	// Process an Integer
	protected int inNumber(String prompt) {
		while (true) {
			inputFlush();
			try {
				prompt(prompt);
				return Integer.parseInt(inString().trim());
			}

			catch (NumberFormatException e) {
				System.out.println("Invalid input. Not a number");
			}
		}
	}

	// Process Integers
	protected List<Integer> inNumbers(String prompt) {
		List<Integer> options = new ArrayList<Integer>();
		inputFlush();
		while (true) {
			prompt(prompt);
			try {
				String[] tokens = inString().split(" ");
				for (String token : tokens) {
					token.trim();
					options.add(Integer.parseInt(token));
				}
				return options;
			} catch (NumberFormatException e) {
				System.out.println("Invalid input. Only numbers separated by space.");
			}
		}
	}


	protected void crawlTolo() {
		String domain = "http://www.tolo.ro/";
		List<String> sites = new ArrayList<String>();
		printMenu3();
		List<Integer> optionsSelected = inNumbers("Enter multiple choices: ");

		for (Integer option : optionsSelected) {
			switch (option.intValue()) {
			case 1: // Football
				sites.add("http://www.tolo.ro/blog/fotbal/");
				break;
			case 2: // Sports
				sites.add("http://www.tolo.ro/blog/sporturi/");
				break;
			case 3: // Investigations
				sites.add("http://www.tolo.ro/blog/investigatii/");
				break;
			case 4: // Media
				sites.add("http://www.tolo.ro/blog/media/");
				break;
			case 5: // Miscellaneous
				sites.add("http://www.tolo.ro/blog/diverse/");
				break;
			case 6: // Recommendations
				sites.add("http://www.tolo.ro/blog/recomandari/");
				break;

			default:
				break;
			}
		}
		System.out.println("Crawling " + Globals.TOLO);
		master.tell(new CrawlNewsSite(NewsAgencies.TOLO, domain, sites), actorSystem.guardian());
	}
	
	protected void crawlEvz() {
		String domain = "http://evz.ro";
		List<String> sites = new ArrayList<String>();
		printMenu4();
		List<Integer> optionsSelected = inNumbers("Enter multiple choices: ");

		for (Integer option : optionsSelected) {
			switch (option.intValue()) {
			case 1: // Politics
				sites.add("http://evz.ro/politica");
				break;
			case 2: // Economics
				sites.add("http://evz.ro/economie");
				break;
			case 3: // Sports
				sites.add("http://evz.ro/sport");
				break;
			case 4: // Culture
				sites.add("http://evz.ro/cultura");
				break;
			case 5: // International
				sites.add("http://evz.ro/international");
				break;
			case 6: // Social
				sites.add("http://evz.ro/social");
				break;
			case 7: // Entertainment
				sites.add("http://evz.ro/monden");
				break;

			default:
				break;
			}
		}
		System.out.println("Crawling " + Globals.EVZ);
		master.tell(new CrawlNewsSite(NewsAgencies.EVZ, domain, sites), actorSystem.guardian());
	}
	
	protected void crawlDigi24() {
		String domain = "https://www.digi24.ro/";
		List<String> sites = new ArrayList<String>();
		printMenu5();
		List<Integer> optionsSelected = inNumbers("Enter multiple choices: ");

		for (Integer option : optionsSelected) {
			switch (option.intValue()) {
			case 1: // Politics
				sites.add("https://www.digi24.ro/stiri/actualitate/politica");
				break;
			case 2: // Economics
				sites.add("https://www.digi24.ro/stiri/economie");
				break;
			case 3: // Sports
				sites.add("https://www.digi24.ro/stiri/sport");
				break;
			case 4: // Health
				sites.add("https://www.digi24.ro/stiri/actualitate/sanatate");
				break;
			case 5: // International
				sites.add("https://www.digi24.ro/stiri/externe");
				break;
			case 6: // Social
				sites.add("https://www.digi24.ro/stiri/actualitate/social");
				break;

			default:
				break;
			}
		}
		System.out.println("Crawling " + Globals.DIGI24);
		master.tell(new CrawlNewsSite(NewsAgencies.DIGI24, domain, sites), actorSystem.guardian());
	}

	public void close() {
		actorSystem.terminate();
	}

}
