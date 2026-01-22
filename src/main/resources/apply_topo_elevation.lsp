(defun c:AssignTopoElevation ( / ss i ent elevVal obj )

  (prompt "\n⛰️ Assigning elevations to polylines from OD:topo...")

  ;; Select all LWPOLYLINEs on V-TOPO-MINR
  (setq ss (ssget "X" '((0 . "LWPOLYLINE"))))

  (if (not ss)
    (prompt "\n⚠️ No polylines found on V-TOPO-MINR.")
    (progn
      (setq i 0)
      (while (< i (sslength ss))
        (setq ent (ssname ss i))

        ;; Attempt to get the elevation from the 'topo' table
        (setq elevVal (ade_odgetfield ent "topo" "Elevation" 0))

        (if elevVal
          (progn
            (setq obj (vlax-ename->vla-object ent))
            (if (numberp elevVal)
              (vla-put-elevation obj elevVal)
              (vla-put-elevation obj (atof elevVal))
            )
          )
        )

        (setq i (1+ i))
      )
      (prompt "\n✅ Elevation assignment complete.")
    )
  )
  (princ)
)
