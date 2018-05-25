/**
 * 
 */
package ro.brad.akka.crawler.examples;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Logger;


import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Cancellable;
import akka.dispatch.OnSuccess;
import akka.pattern.Patterns;
import akka.util.Timeout;
import ro.brad.akka.crawler.actor.MasterActor;
import ro.brad.akka.crawler.actor.ActorMessages;
import ro.brad.akka.crawler.actor.ActorMessages.CrawlNewsSite;
import ro.brad.akka.crawler.actor.ActorMessages.KillCrawl;
import ro.brad.akka.crawler.actor.ActorMessages.SearchText;
import ro.brad.akka.crawler.model.Globals;
import ro.brad.akka.crawler.model.Globals.CrawlingType;
import ro.brad.akka.crawler.model.Globals.IndexOpenMode;
import ro.brad.akka.crawler.model.Globals.NewsAgencies;
import scala.concurrent.ExecutionContext;
import scala.concurrent.Future;
import scala.concurrent.duration.Duration;
import scala.concurrent.duration.FiniteDuration;

/**
 * @author marius
 *
 */
public class RunApp1 {

	public static void main(String[] args) {
		RunApp1 instance = new RunApp1();
		instance.runThisApp();
	}

	public void runThisApp() {

		Lock lock1 = new ReentrantLock();
		Condition notSearched = lock1.newCondition();
		lock1.lock();

		final Logger log = Logger.getLogger("App");
		final ActorSystem actorSystem = ActorSystem.create(Globals.ACTOR_SYSTEM);
		log.info("Actor System " + Globals.ACTOR_SYSTEM + " created");
		final ActorRef master = actorSystem.actorOf(MasterActor.props(IndexOpenMode.CREATE, CrawlingType.NEWS),
				Globals.MASTER_ACTOR);

		// 3 Use Cases
		int hitsCount = 10;
//		String domain = "http://evz.ro";
//		NewsAgencies agency = NewsAgencies.EVZ;
//		List<String> sites = new ArrayList<String>();
//		sites.add("http://evz.ro/politica");
//		sites.add("http://evz.ro/justitie");
//		sites.add("http://evz.ro/economie");
//		String searchText = "Liviu Dragnea";

//		 String domain = "http://www.tolo.ro/";
//		 NewsAgencies agency = NewsAgencies.TOLO;
//		 List<String> sites = new ArrayList<String>();
//		 sites.add("http://www.tolo.ro/blog/sporturi/");
//		 sites.add("http://www.tolo.ro/blog/investigatii/");
//		 String searchText = "FCSB Steaua";

		 String domain = "https://www.digi24.ro/";
		 NewsAgencies agency = NewsAgencies.DIGI24;
		 List<String> sites = new ArrayList<String>();
		 sites.add("https://www.digi24.ro/stiri/externe");
		 sites.add("https://www.digi24.ro/stiri/actualitate/social");
		 sites.add("https://www.digi24.ro/stiri/actualitate/politica");
		 String searchText = "Iohannis suspendare";

		master.tell(new CrawlNewsSite(agency, domain, sites), actorSystem.guardian());

		// Put thread on hold for 1 minute
		try {
			Thread.sleep(30 * 1000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		master.tell(new KillCrawl(domain), actorSystem.guardian());

		ExecutionContext ec = actorSystem.dispatcher();
		FiniteDuration initialDelay = Duration.create(5, TimeUnit.SECONDS);
		FiniteDuration interval = Duration.create(10, TimeUnit.SECONDS);
		Timeout timeout = new Timeout(Duration.create(5, TimeUnit.SECONDS));

		Cancellable askJob = actorSystem.scheduler()
				.schedule(initialDelay, interval, new Runnable() {

					@SuppressWarnings("deprecation")
					public void run() {
						Future<Object> future = Patterns.ask(master, ActorMessages.IS_CRAWLING_SESSION_FINISHED, timeout);
						future.onSuccess(new OnSuccess<Object>() {

							public void onSuccess(Object result) {
								Lock lock2 = new ReentrantLock();
								Condition searched = lock2.newCondition();

								lock2.lock();
								if (result instanceof Boolean) {
									if (((Boolean) result).booleanValue() == true) {
										master.tell(new SearchText(searchText, hitsCount), actorSystem.guardian());
										// Abort the scheduling of runnables
										searched.signal();
									}
								}
								lock2.unlock();
							}
						}, ec);
					}
				}, ec);

		try {
			notSearched.await(10, TimeUnit.SECONDS);
		} catch (InterruptedException e1) {
			e1.printStackTrace();
		} finally {
			askJob.cancel();
			lock1.unlock();
		}
	}
}
