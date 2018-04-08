package de.pixart.messenger.ui.adapter;

import android.content.Context;
import android.support.annotation.NonNull;
import android.widget.ArrayAdapter;
import android.widget.Filter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;

public class KnownHostsAdapter extends ArrayAdapter<String> {
    private ArrayList<String> domains;
    private Filter domainFilter = new Filter() {

        @Override
        protected FilterResults performFiltering(CharSequence constraint) {
            if (constraint != null) {
                ArrayList<String> suggestions = new ArrayList<>();
                final String[] split = constraint.toString().split("@");
                if (split.length == 1) {
                    for (String domain : domains) {
                        suggestions.add(split[0].toLowerCase(Locale
                                .getDefault()) + "@" + domain);
                    }
                } else if (split.length == 2) {
                    for (String domain : domains) {
                        if (domain.contentEquals(split[1])) {
                            suggestions.clear();
                            break;
                        } else if (domain.contains(split[1])) {
                            suggestions.add(split[0].toLowerCase(Locale
                                    .getDefault()) + "@" + domain);
                        }
                    }
                } else {
                    return new FilterResults();
                }
                FilterResults filterResults = new FilterResults();
                filterResults.values = suggestions;
                filterResults.count = suggestions.size();
                return filterResults;
            } else {
                return new FilterResults();
            }
        }

        @Override
        protected void publishResults(CharSequence constraint, FilterResults results) {
            ArrayList filteredList = (ArrayList) results.values;
            if (results.count > 0) {
                clear();
                for (Object c : filteredList) {
                    add((String) c);
                }
                notifyDataSetChanged();
            }
        }
    };

    public KnownHostsAdapter(Context context, int viewResourceId, Collection<String> mKnownHosts) {
        super(context, viewResourceId, new ArrayList<>());

        if (mKnownHosts == null) {
            domains = new ArrayList<>();
        } else {
            domains = new ArrayList<>(mKnownHosts);
        }

        HashSet<String> hashSet = new HashSet<>();

        // get servers from https://conversations.im/compliance/
        new Thread(() -> {
            domains.add("pix-art.de");
            domains.add("conversations.im");
            domains.add("jabber.cat");
            domains.add("jabjab.de");
            domains.add("im.koderoot.net");
            domains.add("riotcat.org");
            domains.add("magicbroccoli.de");
            domains.add("kode.im");
            domains.add("jabber-germany.de");
            domains.add("simplewire.de");
            domains.add("suchat.org");
            domains.add("jabber.at");
            domains.add("trashserver.net");
            domains.add("wiuwiu.de");
            domains.add("5222.de");
            domains.add("dismail.de");
            domains.add("chat.sum7.eu");
            domains.add("xmpp.zone");
            domains.add("libranet.de");
            domains.add("laborversuch.de");
            domains.add("creep.im");
            domains.add("jabber.systemausfall.org");
            domains.add("jabber.hot-chilli.net");
            domains.add("jabber.fr");
            domains.add("jabber.de");
            domains.add("draugr.de");
            domains.add("elaon.de");
            domains.add("high-way.me");
            domains.add("jabber.rwth-aachen.de");
            domains.add("deshalbfrei.org");
            domains.add("mail.de");
            domains.add("bommboo.de");
            domains.add("jabber.systemli.org");
            domains.add("jabb.im");
            domains.add("mailbox.org");
            domains.add("hot-chilli.net");
            domains.add("jabberpl.org");
            domains.add("chinwag.im");
            domains.add("tchncs.de");
            domains.add("zsim.de");
            domains.add("patchcord.be");
            domains.add("gajim.org");
            domains.add("talker.to");
            domains.add("pimux.de");
            domains.add("jabber.home.vdlinde.org");
            domains.add("im.apinc.org");
            domains.add("chatme.im");
            domains.add("fusselkater.org");
            domains.add("datenknoten.me");
            domains.add("fysh.in");
            domains.add("jabber.chaos-darmstadt.de");
            domains.add("yax.im");
            domains.add("neko.im");
            domains.add("jabberzac.org");
            domains.add("xmpp.is");
            domains.add("home.zom.im");
            domains.add("jabber.ccc.de");
            domains.add("jwchat.org");
            domains.add("kdetalk.net");
            domains.add("kde.org");
            domains.add("riseup.net");
            domains.add("ruhr-uni-bochum.de");
            domains.add("njs.netlab.cz");
            domains.add("schokokeks.org");
            domains.add("jabber.cz");
            domains.add("ubuntu-jabber.de");
            domains.add("xabber.de");
            domains.add("ubuntu-jabber.net");
            domains.add("jabber.ru");
            domains.add("darknet.nz");
            domains.add("movim.eu");
            domains.add("404.city");
            domains.add("igniterealtime.org");
            domains.add("kapsi.fi");
            domains.add("jabbel.net");
            domains.add("joindiaspora.com");
            domains.add("alpha-labs.net");
            domains.add("xmppnet.de");
            domains.add("hoth.one");
            domains.add("blah.im");
            domains.add("xmpp.jp");
            domains.add("jabber.uni-mainz.de");
            domains.add("richim.org");
            domains.add("tigase.im");
            domains.add("jappix.com");
            domains.add("member.fsf.org");
            domains.add("jabber.rueckgr.at");
            domains.add("swissjabber.ch");
            domains.add("twattle.net");
            domains.add("jabber.calyxinstitute.org");
            domains.add("sapo.pt");
            domains.add("uprod.biz");
            domains.add("krautspace.de");
            domains.add("kraut.space");
            domains.add("null.pm");
            domains.add("anonymitaet-im-inter.net");
            domains.add("0nl1ne.at");
            domains.add("linuxlovers.at");
            domains.add("jabber.org");
            domains.add("jabber.no-sense.net");
            domains.add("swissjabber.eu");
            domains.add("swissjabber.org");
            domains.add("swissjabber.de");
            domains.add("swissjabber.li");
            domains.add("jabber.no");
            domains.add("cypherpunks.it");
            domains.add("adastra.re");
            domains.add("jabber-br.org");
            domains.add("einfachjabber.de");
            domains.add("jabber.smash-net.org");
            domains.add("freifunk.im");
            domains.add("openmailbox.org");
            domains.add("jabber.otr.im");
            domains.add("evil.im");
            domains.add("xmpp.slack.com");
            domains.add("chat.hipchat.com");
            domains.add("googlemail.com");

            hashSet.addAll(domains);
            domains.clear();
            domains.addAll(hashSet);
            Collections.sort(domains, String::compareToIgnoreCase);
        }).start();
    }

    public KnownHostsAdapter(Context context, int viewResourceId) {
        super(context, viewResourceId, new ArrayList<>());
        domains = new ArrayList<>();
    }

    public void refresh(Collection<String> knownHosts) {
        domains = new ArrayList<>(knownHosts);
        notifyDataSetChanged();
    }

    @Override
    @NonNull
    public Filter getFilter() {
        return domainFilter;
    }
}
