(defun c:EnableLinetypeGeneration ( / ss i ent obj )

  (prompt "\n🧵 Enabling linetype generation for all LWPOLYLINEs...")

  ;; Select all LWPOLYLINEs in the drawing
  (setq ss (ssget "X" '((0 . "LWPOLYLINE"))))

  (if (not ss)
    (prompt "\n⚠️ No LWPOLYLINEs found.")
    (progn
      (setq i 0)
      (while (< i (sslength ss))
        (setq ent (ssname ss i))
        (setq obj (vlax-ename->vla-object ent))
        (vla-put-LinetypeGeneration obj :vlax-true)
        (setq i (1+ i))
      )
      (prompt "\n✅ Linetype generation enabled for all polylines.")
    )
  )
  (princ)
)
