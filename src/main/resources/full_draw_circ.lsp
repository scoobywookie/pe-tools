(defun c:FullAddressCircle ()
  (command "FILEDIA" 0)  ; Suppress dialogs

  ;; Define paths
  (setq scriptFile "C:\\Users\\danie\\Desktop\\CAD-IMPORTS\\circle_layers.scr")
  (setq pyPath "C:\\Users\\danie\\Desktop\\Daniel\\Python Projects\\drawcircleapp\\address_to_scr.py")

  ;; Prompt user for address
  (setq address (getstring T "\nEnter address: "))

  ;; Prompt whether to load map layers
  (initget "Yes No")
  (setq ans (getkword "\nLoad map layers? [Yes/No] <No>: "))
  (if (or (null ans) (/= ans "Yes"))
    (setq downloadFlag "no")
    (setq downloadFlag "yes")
  )

  ;; Delete any old script file
  (if (findfile scriptFile)
    (vl-file-delete scriptFile)
  )

  ;; Record the current last entity
  (setq before (entlast))

  ;; Run Python script via SHELL (blocking)
  (command "_SHELL" (strcat "python \"" pyPath "\" \"" address "\" \"" downloadFlag "\""))

  ;; Wait for the new script to appear
  (while (not (findfile scriptFile))
    (vl-cmdf "._delay" 100)
  )

  ;; If new script exists, run it
  (if (findfile scriptFile)
    (progn
      (command "SCRIPT" (strcat "\"" scriptFile "\""))
      (prompt "\n✅ Script executed.")
      (vl-cmdf "._delay" 500)

      ;; Get the new last object
      (setq after (entlast))

      Verify and zoom to new circle
      (if (and after (not (eq before after)))
        (progn
          (setq entData (entget after))
          (if (= (cdr (assoc 0 entData)) "CIRCLE")
            (progn
              (setq x (car (cdr (assoc 10 entData))))
              (setq y (cadr (cdr (assoc 10 entData))))
              (setq radius (cdr (assoc 40 entData)))
              (command "ZOOM" "C" (list x y) (2 * radius))
            )
            (prompt "\n❌ Last object is not a circle.")
          )
        )
        (prompt "\n❌ No new object found or same as previous.")
      )
    )
    (prompt "\n❌ circle_layers.scr not found — script may have failed.")
  )
  (command "FILEDIA" 1)
  (princ)
)
