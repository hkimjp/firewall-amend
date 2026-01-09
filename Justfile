set dotenv-load

help:
	just --list

# nrepl at 1667
# use clojure sublimed
nrepl:
	bb nrepl-server

systemd:
	sudo cp systemd/fw_amend.* /lib/systemd/system
	sudo systemctl daemon-reload
	sudo systemctl fw_amend.timer restart

# socket repl at 1666
# does not work well with sublime
# socket-repl:
# 	bb socket-repl

install:
  tar cf - . | (cd /opt/firewall-amend && sudo tar xf -)

