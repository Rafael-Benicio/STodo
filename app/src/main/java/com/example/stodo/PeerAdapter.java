package com.example.stodo;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.stodo.sync.SyncManager;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Adapter for displaying discovered peers and running manual ping operations.
 */
public class PeerAdapter extends RecyclerView.Adapter<PeerAdapter.PeerViewHolder> {
    private final List<STodoApplication.DiscoveredServer> peers;
    private final SyncManager syncManager;
    private final Map<String, String> pingStatuses = new HashMap<>();

    /**
     * Constructs a PeerAdapter instance.
     * Example: PeerAdapter adapter = new PeerAdapter(peers, syncManager);
     */
    public PeerAdapter(List<STodoApplication.DiscoveredServer> peers, SyncManager syncManager) {
        this.peers = peers;
        this.syncManager = syncManager;
    }

    /**
     * Creates a new ViewHolder instance for a peer item.
     * Example: onCreateViewHolder(parent, viewType);
     */
    @NonNull
    @Override
    public PeerViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_peer, parent, false);
        return new PeerViewHolder(view);
    }

    /**
     * Binds peer data and registers actions to the ViewHolder.
     * Example: onBindViewHolder(holder, position);
     */
    @Override
    public void onBindViewHolder(@NonNull PeerViewHolder holder, int position) {
        STodoApplication.DiscoveredServer peer = peers.get(position);
        holder.name.setText(peer.getName());
        holder.address.setText(String.format("%s:%d", peer.getHost(), peer.getPort()));
        
        String status = pingStatuses.get(peer.getName());
        holder.status.setText(status != null ? status : "Status: Not pinged");

        holder.btnPing.setOnClickListener(v -> performPing(peer, holder));
    }

    private void performPing(STodoApplication.DiscoveredServer peer, PeerViewHolder holder) {
        holder.btnPing.setEnabled(false);
        pingStatuses.put(peer.getName(), "Status: Ping in progress...");
        notifyItemChanged(holder.getAdapterPosition());

        syncManager.ping(peer.getHost(), peer.getPort(), new SyncManager.PingCallback() {
            @Override
            public void onSuccess(long latencyMs) {
                pingStatuses.put(peer.getName(), "Status: Online (Ping: " + latencyMs + "ms)");
                notifyItemChanged(holder.getAdapterPosition());
            }

            @Override
            public void onError(String message) {
                pingStatuses.put(peer.getName(), "Status: Offline (" + message + ")");
                notifyItemChanged(holder.getAdapterPosition());
            }
        });
    }

    /**
     * Returns the size of the peer list.
     * Example: int count = getItemCount();
     */
    @Override
    public int getItemCount() {
        int size = peers.size();
        return size;
    }

    /**
     * ViewHolder representation for a single peer row.
     */
    public static class PeerViewHolder extends RecyclerView.ViewHolder {
        TextView name;
        TextView address;
        TextView status;
        Button btnPing;

        /**
         * Constructs a PeerViewHolder instance.
         * Example: PeerViewHolder holder = new PeerViewHolder(view);
         */
        public PeerViewHolder(@NonNull View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.textViewPeerName);
            address = itemView.findViewById(R.id.textViewPeerAddress);
            status = itemView.findViewById(R.id.textViewPingStatus);
            btnPing = itemView.findViewById(R.id.buttonPing);
        }
    }
}
