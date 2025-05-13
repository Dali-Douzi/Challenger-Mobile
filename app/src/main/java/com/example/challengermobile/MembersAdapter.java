package com.example.challengermobile;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Spinner;
import android.widget.ArrayAdapter;
import android.widget.AdapterView;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class MembersAdapter extends RecyclerView.Adapter<MembersAdapter.MemberViewHolder> {

    public interface RoleChangeListener {
        void onRoleChanged(String memberId, String newRole);
    }

    public interface KickMemberListener {
        void onKickMember(String memberId);
    }

    private List<Member> memberList;
    private boolean isOwner;
    private RoleChangeListener roleChangeListener;
    private KickMemberListener kickMemberListener;

    public MembersAdapter(List<Member> memberList,
                          boolean isOwner,
                          RoleChangeListener roleChangeListener,
                          KickMemberListener kickMemberListener) {
        this.memberList        = memberList;
        this.isOwner           = isOwner;
        this.roleChangeListener = roleChangeListener;
        this.kickMemberListener = kickMemberListener;
    }

    public void updateList(List<Member> newList) {
        this.memberList = newList;
        notifyDataSetChanged();
    }

    @NonNull @Override
    public MemberViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View item = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_member, parent, false);
        return new MemberViewHolder(item);
    }

    @Override
    public void onBindViewHolder(@NonNull MemberViewHolder holder, int position) {
        Member m = memberList.get(position);
        holder.tvName.setText(m.getName());
        holder.tvRole.setText(m.getRole());

        // Role spinner logic (unchanged)
        if (isOwner) {
            holder.tvRole.setVisibility(View.GONE);
            holder.spinnerRole.setVisibility(View.VISIBLE);
            ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                    holder.itemView.getContext(),
                    R.array.team_role_list,
                    android.R.layout.simple_spinner_item
            );
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            holder.spinnerRole.setAdapter(adapter);
            int posn = adapter.getPosition(m.getRole());
            holder.spinnerRole.setSelection(posn);
            holder.spinnerRole.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override public void onItemSelected(AdapterView<?> p, View v, int p2, long id) {
                    String newRole = p.getItemAtPosition(p2).toString();
                    if (!newRole.equals(m.getRole())) {
                        m.setRole(newRole);
                        roleChangeListener.onRoleChanged(m.getId(), newRole);
                    }
                }
                @Override public void onNothingSelected(AdapterView<?> p) {}
            });
        } else {
            holder.tvRole.setVisibility(View.VISIBLE);
            holder.spinnerRole.setVisibility(View.GONE);
        }

        // Kick button only for owner and non-owner rows
        if (isOwner && !"Owner".equals(m.getRole())) {
            holder.btnKick.setVisibility(View.VISIBLE);
            holder.btnKick.setOnClickListener(v ->
                    kickMemberListener.onKickMember(m.getId())
            );
        } else {
            holder.btnKick.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return memberList.size();
    }

    public List<Member> getMemberList() { return memberList; }

    static class MemberViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvRole;
        Spinner spinnerRole;
        Button btnKick;

        MemberViewHolder(View view) {
            super(view);
            tvName      = view.findViewById(R.id.tvMemberName);
            tvRole      = view.findViewById(R.id.tvMemberRole);
            spinnerRole = view.findViewById(R.id.spinnerMemberRole);
            btnKick     = view.findViewById(R.id.btnKickMember);
        }
    }
}