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
public class RunApp2 {

	public static void main(String[] args) {
		RunApp2 instance = new RunApp2();
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

		String domain1 = "https://www.digi24.ro/";
		NewsAgencies agency1 = NewsAgencies.DIGI24;
		List<String> sites1 = new ArrayList<String>();
		sites1.add("https://www.digi24.ro/stiri/externe");
		sites1.add("https://www.digi24.ro/stiri/actualitate/social");
		sites1.add("https://www.digi24.ro/stiri/actualitate/politica");

		String domain2 = "http://www.tolo.ro/";
		NewsAgencies agency2 = NewsAgencies.TOLO;
		List<String> sites2 = new ArrayList<String>();
		sites2.add("http://www.tolo.ro/blog/diverse/");
		sites2.add("http://www.tolo.ro/blog/investigatii/");

		String domain3 = "http://evz.ro";
		NewsAgencies agency3 = NewsAgencies.EVZ;
		List<String> sites3 = new ArrayList<String>();
		sites3.add("http://evz.ro/politica");
		sites3.add("http://evz.ro/justitie");
		sites3.add("http://evz.ro/economie");

		String searchText = "Liviu Dragnea DNA";
		int hitsCount = 10;

		master.tell(new CrawlNewsSite(agency1, domain1, sites1), actorSystem.guardian());

		try {
			Thread.sleep(10 * 1000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		master.tell(new CrawlNewsSite(agency2, domain2, sites2), actorSystem.guardian());

		try {
			Thread.sleep(10 * 1000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		master.tell(new CrawlNewsSite(agency3, domain3, sites3), actorSystem.guardian());

		try {
			Thread.sleep(40 * 1000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		master.tell(new KillCrawl(domain2), actorSystem.guardian());

		try {
			Thread.sleep(20 * 1000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		master.tell(new KillCrawl(domain1), actorSystem.guardian());

		try {
			Thread.sleep(20 * 1000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		master.tell(new KillCrawl(domain3), actorSystem.guardian());
		
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
