resource "null_resource" "setup_nodes" {
  triggers = {
    always_run = timestamp()
  }

  provisioner "local-exec" {
    command = <<EOT
      kubectl label nodes desktop-worker type=database --overwrite
      kubectl taint nodes desktop-worker database=true:NoSchedule --overwrite

      kubectl label nodes desktop-worker2 type=messaging --overwrite
      kubectl taint nodes desktop-worker2 kafka=true:NoSchedule --overwrite

      kubectl label nodes desktop-worker3 type=application --overwrite
      kubectl taint nodes desktop-worker3 application=true:NoSchedule --overwrite

      kubectl label nodes desktop-worker4 type=monitoring --overwrite
      kubectl taint nodes desktop-worker4 monitoring=true:NoSchedule --overwrite
    EOT
  }

  depends_on = [kubectl_manifest.namespaces]
}