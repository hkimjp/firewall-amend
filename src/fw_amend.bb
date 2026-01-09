#!/usr/bin/env bb
;(ns fw-amend)
(require '[babashka.process :as ps])
(require '[taoensso.timbre :as t])

(def never-delete (System/getenv "NEVER_DELETE"))
(def target (System/getenv "TARGET"))

(t/log "never-delette:" never-delete)
(t/log "target:" target)

(defn shell [args]
  (-> (ps/shell {:out :string} args)
      :out))

(defn ufw-cur []
  (->> (shell "sudo ufw status")
       str/split-lines
       (filter #(str/starts-with? % "Anywhere"))
       (remove #(re-find (re-pattern never-delete) %))
       last
       (re-find #"\d+\.\d+\.\d+\.\d+")))

(defn ufw-find [ip]
  (re-find (re-pattern ip)
           (->> (shell "sudo ufw status")
                concat
                (apply str))))

(defn ufw-delete [ip]
  (shell (str "sudo ufw delete allow from " ip)))

(defn ufw-allow [ip]
  (shell (str "sudo ufw allow from " ip)))

(defn ufw-update [old new]
  (if (ufw-find old)
    (try
      (ufw-delete old)
      (ufw-allow new)
      (t/info "ufw-update:" old "->" new)
      (catch Exception e
        (t/error (.getMessage e))))
    (t/error "ufw-update: did not find" old)))

; (ufw-update "2.3.4.50" "1.2.3.4")

(defn f2b-update [old new]
  (let [cmd (str "sudo sed -i.bak "
                 (format "'s/%s/%s/'" old new)
                 " /etc/fail2ban/jail.d/local.conf")]
    (try
      (shell cmd)
      (shell "sudo service fail2ban restart")
      (t/info "f2b-update:" old "->" new)
      (catch Exception e
        (t/error "f2b-update" (.getMessage e))
        (t/error "cmd:" cmd)))))

; (f2b-update "1.2.3.4" "5.6.7.8")

(defn dig [url]
  (-> (shell (str "dig +short " url))
      str/trim))

(defn fw-amend [host]
  (let [cur-ip (dig host)
        ufw-ip (ufw-cur)]
    (if (= cur-ip ufw-ip)
      (t/info "fw-amend: no change")
      (do
        (ufw-update ufw-ip cur-ip)
        (f2b-update ufw-ip cur-ip)
        (t/info "fw-amend: changed")))))

(fw-amend target)
