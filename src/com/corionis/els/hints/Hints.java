package com.corionis.els.hints;

import com.corionis.els.gui.browser.NavTreeUserObject;
import com.corionis.els.Context;
import com.corionis.els.MungeException;
import com.corionis.els.Utils;
import com.corionis.els.gui.hints.HintsTableModel;
import com.corionis.els.repository.Library;
import com.corionis.els.repository.Repository;
import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;

import java.io.PrintWriter;
import java.lang.reflect.Type;
import java.util.*;

/**
 * Hints class to handle writing, updating, finding and executing ELS Hints and their commands.
 */
public class Hints
{
    private final Marker SHORT = MarkerManager.getMarker("SHORT");
    private final Marker SIMPLE = MarkerManager.getMarker("SIMPLE");
    private final Logger logger = LogManager.getLogger("applog");
    private Context context;
    private int executedHints = 0;
    private Gson gsonBuilder = new GsonBuilder().create();
    private Gson gsonParser = new Gson();
    private HintKeys keys;

    /**
     * Constructor
     *
     * @param context  The Context
     * @param hintKeys HintKeys if enabled, else null
     */
    public Hints(Context context, HintKeys hintKeys)
    {
        this.context = context;
        this.keys = hintKeys;
    }

    public ArrayList<Hint> checkConflicts(String library, String itemPath) throws Exception
    {
        ArrayList<Hint> results = new ArrayList<Hint>();
        if (context.cfg.isRemoteStatusServer())
        {
            String line = "conflict " + "\"" + library + "\" \"" + itemPath + "\"";
            String response = context.hintsStty.roundTrip(line + "\n", null, 10000);
            if (!response.toLowerCase().equals("false"))
            {
                results = gsonParser.fromJson(response, results.getClass());
            }
        }
        else
        {
            Hint conflicter = new Hint();
            conflicter.fromLibrary = library;
            conflicter.fromItemPath = itemPath;
            results = context.datastore.getAll(conflicter, "conflict");
        }
        return results;
    }

    /**
     * Execute a Hint
     *
     * @param repo Repository target for Hint
     * @param hint The Hint
     * @return "true" or "false" success
     * @throws Exception
     */
    private String execute(Repository repo, Hint hint) throws Exception
    {
        int forIndex = -2;
        String result = "false";
        boolean success = false;

        HintKey hk = context.hintsHandler.findHintKey(repo);
        forIndex = hint.isFor(hk.system);
        if (forIndex >= -1)
        {
            Library fromLib = repo.getLibrary(hint.fromLibrary);
            if (fromLib.items == null || fromLib.itemMap == null || fromLib.rescanNeeded)
                repo.scan(fromLib.name);

            if (hint.toLibrary != null && hint.toLibrary.length() > 0)
            {
                Library toLib = repo.getLibrary(hint.toLibrary);
                if (toLib.items == null || toLib.itemMap == null || toLib.rescanNeeded)
                    repo.scan(toLib.name);
            }

            if (hint.action.trim().toLowerCase().equals("mv"))
            {
                success = context.transfer.move(repo, hint);
            }
            else if (hint.action.trim().toLowerCase().equals("rm"))
            {
                success = context.transfer.remove(repo, hint);
            }
            else
                throw new MungeException(context.cfg.gs("Hints.action.must.be.mv.or.rm"));

            if (success)
                result = "true";
        }
        return result;
    }

    /**
     * Find the hint UUID key for a Repository
     *
     * @param repo Repository containing the UUID key to find
     * @return Hints.Hintkey of matching UUID
     * @throws Exception if not found
     */
    public HintKey findHintKey(Repository repo)
    {
        // find the ELS key for this repo
        HintKey hintKey = keys.findKey(repo.getLibraryData().libraries.key);
        return hintKey;
    }

    public ArrayList<Hint> getAll() throws Exception
    {
        ArrayList<Hint> results = null;
        if (context.cfg.isHintTrackingEnabled())
        {
            if (context.cfg.isRemoteStatusServer())
            {
                String line = "get \"all\"";
                String response = context.hintsStty.roundTrip(line + "\n", null, 10000);
                if (!response.toLowerCase().equals("false"))
                {
                    Type listType = new TypeToken<ArrayList<Hint>>()
                    {
                    }.getType();
                    results = gsonParser.fromJson(response, listType);
                }
            }
            else
            {
                if (context.datastore.hints != null)
                {
                    results = new ArrayList<Hint>();
                    results.addAll(context.datastore.hints);
                }
            }
        }
        return results;
    }

    public int getCount(String system) throws Exception
    {
        int count = 0;
        if (context.cfg.isHintTrackingEnabled())
        {
            if (context.cfg.isRemoteStatusServer())
            {
                String line = "count \"" + system + "\"";
                String response = context.hintsStty.roundTrip(line + "\n", null, 10000);
                if (!response.toLowerCase().equals("false"))
                {
                    count = Integer.valueOf(response);
                }
            }
            else
            {
                if (context.datastore.hints != null)
                    count = context.datastore.count(system);
            }
        }
        return count;
    }

    public ArrayList<Hint> getFor(String hintSystemName) throws Exception
    {
        ArrayList<Hint> results = null;
        if (context.cfg.isRemoteStatusServer())
        {
            String line = "get \"for\" \"" + hintSystemName + "\"";
            String response = context.hintsStty.roundTrip(line + "\n", null, 10000);
            if (!response.toLowerCase().equals("false"))
            {
                Type listType = new TypeToken<ArrayList<Hint>>()
                {
                }.getType();
                results = gsonParser.fromJson(response, listType);
            }
        }
        else
        {
            if (context.datastore.hints != null)
                results = context.datastore.getFor(hintSystemName);
        }
        return results;
    }

    /**
     * Process Hints<br/>
     * <br/>
     * Used by Process during back-up operations to execute all publisher then subscriber Hints.
     *
     * @return "true", "false" or "fault"
     * @throws Exception
     */
    public String hintsMunge(boolean publisherOnly, PrintWriter mismatches) throws Exception
    {
        // process Hints for publisher first
        String result = hintsMunge(null, true, null, null);

        // rescan of Publisher is needed here for Subscriber Hints next, not repeated in back-up process
        if (!result.toLowerCase().equals("false"))
            rescanLibraries(true);

        if (!publisherOnly) // process Subscriber
        {
            result = hintsMunge(null, false, null, mismatches);
            // Note: Rescans happen for Subscriber during the back-up process if
            // the Subscriber library rescanNeeded is true
        }
        return result;
    }

    /**
     * Process Hints<br/>
     * <br/>
     * Used by Subscriber Listener (stty.subscriber.Daemon). Does not update status.
     * <p>
     * This is a local-only method
     *
     * @param pending
     * @return
     * @throws Exception
     */
    public String hintsMunge(ArrayList<Hint> pending) throws Exception
    {
        HintKey key;
        Repository repo;
        String response = null;

        repo = context.subscriberRepo;

        // participating in Hints?
        key = findHintKey(repo);
        if (key != null && key.system != null)
        {
            if (pending != null && pending.size() > 0)
            {
                for (int i = 0; i < pending.size(); ++i)
                {
                    try
                    {
                        Hint hint = pending.get(i);
                        ++executedHints;
                        logger.info(context.cfg.gs("Hints.executing.hint") + executedHints + ": " + hint.getLocalUtc(context));

                        // execute each locally
                        response = execute(repo, hint);
                    }
                    catch (Exception e)
                    {
                        logger.error(Utils.getStackTrace(e));
                        response = "fault";
                    }
                }
                logger.info(context.cfg.gs("Hints.execution.complete") + response);
            }
        }
        return response;
    }

    /**
     * Process Hints<br/>
     * <br/>
     * Used by Navigator and Publisher back-up operations. Updates Hint status.
     *
     * @param pending
     * @param forMe
     * @param model
     * @return
     * @throws Exception
     */
    public String hintsMunge(ArrayList<Hint> pending, boolean forMe, HintsTableModel model, PrintWriter mismatchesFile) throws Exception
    {
        int falses = 0;
        int faults = 0;
        int forIndex = -2;
        HintKey key;
        int nots = 0;
        Repository repo;
        String response = "false";
        int trues = 0;

        if (forMe)
            repo = context.publisherRepo;
        else
            repo = context.subscriberRepo;

        if (context.cfg.isDryRun())
        {
            logger.info(context.cfg.gs("Hints.skipping.munge.of.hints.to") + repo.getLibraryData().libraries.description + " (--dry-run)");
        }
        else
        {
            logger.info(context.cfg.gs("Hints.munging.hints.to") + repo.getLibraryData().libraries.description);

            // participating in Hints?
            key = findHintKey(repo);
            if (key != null && key.system != null)
            {
                // any Hints? If not process all pending For
                if (pending == null || pending.size() < 1)
                {
                    pending = getFor(key.system);
                }

                if (pending != null && pending.size() > 0)
                {
                    if (mismatchesFile != null)
                    {
                        mismatchesFile.println(context.cfg.gs("Hints.munging.hints.to") + repo.getLibraryData().libraries.description);
                        mismatchesFile.println(" ");
                    }

                    executedHints = 0;
                    for (int i = 0; i < pending.size(); ++i)
                    {
                        Hint hint = pending.get(i);
                        forIndex = hint.isFor(key.system);
                        if (forIndex >= -1)
                        {
                            try
                            {
                                ++executedHints;
                                logger.info(context.cfg.gs("Hints.executing.hint") + executedHints + ": " + hint.getLocalUtc(context));
                                if (!forMe && context.cfg.isRemoteSubscriber())
                                {
                                    // execute each remotely on subscriber
                                    String json = gsonBuilder.toJson(hint);
                                    String line = "\"execute\" " + json;
                                    response = context.clientStty.roundTrip(line + "\n", null, 20000);

                                    if (response.trim().toLowerCase().equals("true"))
                                    {
                                        if (hint.action.trim().toLowerCase().equals("mv")) // move & rename
                                        {
                                            repo.getLibrary(hint.fromLibrary).rescanNeeded = true;
                                            repo.getLibrary(hint.toLibrary).rescanNeeded = true;
                                        }
                                        else // it is a rm with from only
                                            repo.getLibrary(hint.fromLibrary).rescanNeeded = true;
                                    }
                                }
                                else
                                {
                                    // execute each locally
                                    response = execute(repo, hint);
                                }
                            }
                            catch (Exception e)
                            {
                                logger.error(Utils.getStackTrace(e));
                                response = "fault";
                            }

                            String summary = hint.getActionSummary(context, key.system);
                            if (response.trim().toLowerCase().equals("true"))
                            {
                                ++trues;
                                summary = "True, " + summary;
                                hint.setStatus(key.system, "Done");
                            }
                            else if (!response.trim().toLowerCase().equals("false"))
                            {
                                ++faults;
                                summary = "Fault, " + summary;
                                hint.setStatus(key.system, "Fault");
                            }
                            else
                            {
                                ++falses;
                                summary = "False, " + summary;
                                hint.setStatus(key.system, "Done");
                            }

                            if (mismatchesFile != null)
                                mismatchesFile.println(summary);

                            if (model != null)
                                model.fireTableDataChanged();

                            writeOrUpdateHint(hint, key.uuid);
                        }
                        else
                            ++nots;
                    }

                    if (faults > 0)
                    {
                        response = "fault";
                        if (mismatchesFile != null)
                            mismatchesFile.println("Faults: " + faults);
                    }
                    else if (trues > 0)
                        response = "true";
                    else
                        response = "false";

                    logger.info("+------------------------------------------");
                    logger.info(context.cfg.gs("Hints.hint.execution.complete.result") + response);
                    logger.info(context.cfg.gs("Hints.performed") + trues);
                    logger.info(context.cfg.gs("Hints.not.performed") + falses);
                    logger.info(context.cfg.gs("Hints.not.for") + nots);
                    logger.info(context.cfg.gs("Hints.faults") + faults);
                    logger.info(context.cfg.gs("Hints.total") + executedHints);
                    logger.info("+------------------------------------------");

                    if (mismatchesFile != null)
                    {
                        mismatchesFile.println("+------------------------------------------");
                        mismatchesFile.println(context.cfg.gs("Hints.hint.execution.complete.result") + response);
                        mismatchesFile.println(context.cfg.gs("Hints.performed") + trues);
                        mismatchesFile.println(context.cfg.gs("Hints.not.performed") + falses);
                        mismatchesFile.println(context.cfg.gs("Hints.not.for") + nots);
                        mismatchesFile.println(context.cfg.gs("Hints.faults") + faults);
                        mismatchesFile.println(context.cfg.gs("Hints.total") + executedHints);
                        mismatchesFile.println("+------------------------------------------");
                        mismatchesFile.println("");
                    }
                }
            }
        }
        return response;
    }

    private boolean isDone(Hint hint)
    {
        if (hint.statuses != null && hint.statuses.size() > 0)
        {
            // does it have status for all participating Hint Keys?
            if (hint.statuses.size() != context.hintKeys.size())
                return false;

            for (int i = 0; i < hint.statuses.size(); ++i)
            {
                HintStatus hs = hint.statuses.get(i);
                if (!hs.status.trim().toLowerCase().equals("done"))
                {
                    return false;
                }
            }
        }
        else
            return false;
        return true;
    }

    private void rescanLibraries(boolean scanPublisher) throws Exception
    {
        Repository repo;
        if (scanPublisher)
            repo = context.publisherRepo;
        else
            repo = context.subscriberRepo;

        for (Library lib : repo.getLibraryData().libraries.bibliography)
        {
            if (lib.rescanNeeded)
            {
                //logger.info("Rescan required for library: " + lib.name);
                repo.scan(lib.name);
            }
        }
    }

    public String save(ArrayList<Hint> hints) throws Exception
    {
        String response = "true";
        if (context.cfg.isHintTrackingEnabled())
        {
            if (context.cfg.isRemoteStatusServer())
            {
                Type listType = new TypeToken<ArrayList<Hint>>()
                {
                }.getType();
                String json = gsonBuilder.toJson(hints, listType);
                String line = "\"save\" " + json;
                response = context.hintsStty.roundTrip(line + "\n", null, 10000);
            }
            else
            {
                context.datastore.hints = hints;
                context.datastore.write();
            }
        }
        return response;
    }

    public void writeHint(String action, boolean isWorkstation, NavTreeUserObject sourceTuo, NavTreeUserObject targetTuo) throws Exception
    {
        // if a workstation and source is publisher then it is local or a basic add and there is no hint
        if (isWorkstation && !sourceTuo.isSubscriber())
            return;

        boolean sourceIsCollection = sourceTuo.node.getMyTree().getName().toLowerCase().contains("collection");
        boolean targetIsCollection = (targetTuo != null) ? targetTuo.node.getMyTree().getName().toLowerCase().contains("collection") : false;

        // if source is subscriber system tab this it is a basic add, no hint
        if (sourceTuo.isSubscriber() && !sourceIsCollection)
            return;

        // if either the source or target are not a collection there is no hint
        if (sourceIsCollection || targetIsCollection)
        {
            // are Hints being processed for the source system?
            HintKey hk = context.hintKeys.findKey(sourceTuo.node.getMyRepo().getLibraryData().libraries.key);
            if (hk == null)
                return;

            Hint hint = new Hint();
            hint.author = "User";
            hint.system = hk.system;

            String act = action.trim().toLowerCase();
            hint.action = act;

            hint.fromLibrary = sourceTuo.getParentLibrary().getUserObject().name;
            hint.fromItemPath = context.navigator.reduceCollectionPath(sourceTuo);
            hint.directory = sourceTuo.isDir;

            if (act.equals("mv"))
            {
                String moveTo = context.navigator.reduceCollectionPath(targetTuo);

                // do not append right-side target path if the nodes are the same
                if (sourceTuo.node != targetTuo.node)
                {
                    if (moveTo.length() > 0 && !moveTo.trim().endsWith("|"))
                        moveTo += targetTuo.getRepo().getSeparator();
                    moveTo += Utils.getRightPath(sourceTuo.getPath(), sourceTuo.getRepo().getSeparator());
                }

                hint.toLibrary = targetTuo.getParentLibrary().getUserObject().name;
                hint.toItemPath = moveTo;
            }
            else if (!act.equals("rm"))
                throw new MungeException(context.cfg.gs("Hints.action.must.be.mv.or.rm"));

            hint.setStatus(hk.system, "Done");

            String sourceKey = sourceTuo.isSubscriber() ? context.subscriberRepo.getLibraryData().libraries.key : context.publisherRepo.getLibraryData().libraries.key;
            writeOrUpdateHint(hint, sourceKey);
        }
    }

    public boolean writeOrUpdateHint(Hint hint, String sourceKey) throws Exception
    {
        Hint dsHint = null;
        boolean found = false;
        boolean success = false;

        // get any matching Hint
        if (context.cfg.isRemoteStatusServer())
        {
            String json = gsonBuilder.toJson(hint);
            String line = "get \"full\" " + json;
            String response = context.hintsStty.roundTrip(line + "\n", "Sending remote Hint Server: " + line, 10000);
            logger.debug("Hint Server response: " + response);
            if (!response.equals("false"))
            {
                dsHint = gsonParser.fromJson(response, Hint.class);
            }
        }
        else
        {
            dsHint = context.datastore.get(hint, "full");
        }

        if (dsHint != null)
        {
            // existing Hint
            found = true;
            dsHint.copyStatusFrom(hint);
            hint = dsHint;
        }
        else
        {
            // it is a new Hint
            if (sourceKey != null)
            {
                for (HintKey key : context.hintKeys.get())
                {
                    HintStatus stat = new HintStatus();
                    if (key.uuid.equalsIgnoreCase(sourceKey))
                        hint.setStatus(key.system, "Done");
                    else
                        hint.setStatus(key.system, "For");
                }
            } // sourceKey == null is valid for new Hint
        }

        if (context.cfg.isRemoteStatusServer())
        {
            String json = gsonBuilder.toJson(hint);
            String line = "\"hint\" " + json;
            String response = context.hintsStty.roundTrip(line + "\n", "Sending remote Hint Server: " + line, 10000);
            logger.info("Hint Server response: " + response);
            if (response.toLowerCase().equals("true"))
                success = true;
        }
        else
        {
            if (!found)
                context.datastore.add(hint);

            // is it all Done?
            if (isDone(hint))
            {
                context.datastore.hints.remove(hint);
                if (executedHints < 1)
                    executedHints = 1;
                logger.info(context.cfg.gs("Hints.hint.done.and.removed") + executedHints + ": " + hint.getLocalUtc(context) + ", " +
                        context.datastore.hints.size() + " remaining");
            }

            context.datastore.write();
            success = true;
        }

        if (context.cfg.isNavigator())
        {
            context.navigator.checkForHints();
            if (context.navigator.dialogHints != null && context.navigator.dialogHints.isVisible())
                context.navigator.dialogHints.refresh();
        }

        return success;
    }

}
