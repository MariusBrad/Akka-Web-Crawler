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

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Cancellable;
import akka.actor.Props;
import akka.cluster.Cluster;
import akka.dispatch.OnSuccess;
import akka.pattern.Patterns;
import akka.util.Timeout;
import ro.brad.akka.crawler.cluster.ClusterMessages.SearchText;
import ro.brad.akka.crawler.cluster.ClusterMessages;
import ro.brad.akka.crawler.cluster.FrontendActor;
import ro.brad.akka.crawler.cluster.ClusterMessages.CrawlNewsSite;
import ro.brad.akka.crawler.cluster.ClusterMessages.KillCrawl;
import ro.brad.akka.crawler.model.Globals;
import ro.brad.akka.crawler.model.Globals.NewsAgencies;
import scala.concurrent.Await;
import scala.concurrent.ExecutionContext;
import scala.concurrent.Future;
import scala.concurrent.duration.Duration;
import scala.concurrent.duration.FiniteDuration;

/**
 * @author marius
 *
 */
public class ClusterFrontendNode {

	public static void main(String[] args) {

		final Config config = ConfigFactory.parseString("akka.cluster.roles = [frontend]")
				.withFallback(ConfigFactory.load("cluster"));

		final ActorSystem system = ActorSystem.create(Globals.CLUSTER_SYSTEM, config);

		Cluster.get(system)
				.registerOnMemberUp(new Runnable() {
					@Override
					public void run() {
						Lock lock1 = new ReentrantLock();
						Condition notSearched = lock1.newCondition();
						lock1.lock();

						ActorRef frontend = system.actorOf(Props.create(FrontendActor.class), Globals.FRONTEND);

						// Wait 4 seconds for the ClusterStateEvent to be fired
						try {
							Thread.sleep(4 * 1000);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}

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

						frontend.tell(new CrawlNewsSite(agency1, domain1, sites1), system.guardian());

						try {
							Thread.sleep(10 * 1000);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}

						frontend.tell(new CrawlNewsSite(agency2, domain2, sites2), system.guardian());

						try {
							Thread.sleep(10 * 1000);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}

						frontend.tell(new CrawlNewsSite(agency3, domain3, sites3), system.guardian());

						try {
							Thread.sleep(40 * 1000);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}

						frontend.tell(new KillCrawl(domain2), system.guardian());

						try {
							Thread.sleep(20 * 1000);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}

						frontend.tell(new KillCrawl(domain1), system.guardian());

						try {
							Thread.sleep(20 * 1000);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}

						frontend.tell(new KillCrawl(domain3), system.guardian());

						ExecutionContext ec = system.dispatcher();
						FiniteDuration initialDelay = Duration.create(5, TimeUnit.SECONDS);
						FiniteDuration interval = Duration.create(10, TimeUnit.SECONDS);
						Timeout timeout = new Timeout(Duration.create(5, TimeUnit.SECONDS));

						Cancellable askJob = system.scheduler()
								.schedule(initialDelay, interval, new Runnable() {

									@SuppressWarnings("deprecation")
									public void run() {
										Future<Object> future = Patterns.ask(frontend,
												ClusterMessages.IS_CRAWLING_SESSION_FINISHED, timeout);
										future.onSuccess(new OnSuccess<Object>() {

											public void onSuccess(Object result) {
												Lock lock2 = new ReentrantLock();
												Condition searched = lock2.newCondition();

												lock2.lock();
												if (result instanceof Boolean) {
													if (((Boolean) result).booleanValue() == true) {
														frontend.tell(new SearchText(system.provider()
																.getDefaultAddress(), searchText, hitsCount),
																system.guardian());
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
				});

		Cluster.get(system)
				.registerOnMemberRemoved(new Runnable() {
					@Override
					public void run() {
						// exit JVM when ActorSystem has been terminated
						final Runnable exit = new Runnable() {
							@Override
							public void run() {
								System.exit(0);
							}
						};
						system.registerOnTermination(exit);

						// shut down ActorSystem
						system.terminate();

						// In case ActorSystem shutdown takes longer than 10 seconds,
						// exit the JVM forcefully anyway.
						// We must spawn a separate thread to not block current thread,
						// since that would have blocked the shutdown of the ActorSystem.
						new Thread() {
							@Override
							public void run() {
								try {
									Await.ready(system.whenTerminated(), Duration.create(10, TimeUnit.SECONDS));
								} catch (Exception e) {
									System.exit(-1);
								}

							}
						}.start();
					}
				});
	}

}
