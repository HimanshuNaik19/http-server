"use client"

import { useState, useEffect } from "react"
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card"
import { Badge } from "@/components/ui/badge"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs"
import { ScrollArea } from "@/components/ui/scroll-area"
import { Separator } from "@/components/ui/separator"
import { Server, Activity, FileText, Globe, Play, Square, RefreshCw, Clock, Users, HardDrive } from "lucide-react"

interface RequestLog {
  id: string
  timestamp: string
  method: string
  path: string
  status: number
  responseTime: number
  clientIp: string
}

interface ServerStats {
  uptime: string
  totalRequests: number
  activeConnections: number
  memoryUsage: string
}

export default function HttpServerDashboard() {
  const [serverStatus, setServerStatus] = useState<"running" | "stopped">("running")
  const [requestLogs, setRequestLogs] = useState<RequestLog[]>([])
  const [serverStats, setServerStats] = useState<ServerStats>({
    uptime: "2h 34m",
    totalRequests: 1247,
    activeConnections: 8,
    memoryUsage: "45.2 MB",
  })
  const [newRoute, setNewRoute] = useState({ path: "", handler: "" })

  // Simulate real-time request logs
  useEffect(() => {
    if (serverStatus === "running") {
      const interval = setInterval(() => {
        const methods = ["GET", "POST", "PUT", "DELETE"]
        const paths = ["/api/users", "/static/index.html", "/api/data", "/health", "/favicon.ico"]
        const statuses = [200, 201, 404, 500]
        const ips = ["192.168.1.100", "10.0.0.45", "172.16.0.23", "192.168.1.200"]

        const newLog: RequestLog = {
          id: Date.now().toString(),
          timestamp: new Date().toLocaleTimeString(),
          method: methods[Math.floor(Math.random() * methods.length)],
          path: paths[Math.floor(Math.random() * paths.length)],
          status: statuses[Math.floor(Math.random() * statuses.length)],
          responseTime: Math.floor(Math.random() * 500) + 10,
          clientIp: ips[Math.floor(Math.random() * ips.length)],
        }

        setRequestLogs((prev) => [newLog, ...prev.slice(0, 49)]) // Keep last 50 logs
        setServerStats((prev) => ({
          ...prev,
          totalRequests: prev.totalRequests + 1,
          activeConnections: Math.max(1, prev.activeConnections + (Math.random() > 0.5 ? 1 : -1)),
        }))
      }, 2000)

      return () => clearInterval(interval)
    }
  }, [serverStatus])

  const toggleServer = () => {
    setServerStatus((prev) => (prev === "running" ? "stopped" : "running"))
  }

  const getStatusColor = (status: number) => {
    if (status >= 200 && status < 300) return "text-green-600"
    if (status >= 300 && status < 400) return "text-yellow-600"
    if (status >= 400 && status < 500) return "text-orange-600"
    return "text-red-600"
  }

  const addRoute = () => {
    if (newRoute.path && newRoute.handler) {
      // In a real implementation, this would send a request to your Java server
      console.log("Adding route:", newRoute)
      setNewRoute({ path: "", handler: "" })
    }
  }

  return (
    <div className="min-h-screen bg-background p-6">
      <div className="max-w-7xl mx-auto space-y-6">
        {/* Header */}
        <div className="flex items-center justify-between">
          <div className="flex items-center space-x-3">
            <Server className="h-8 w-8 text-primary" />
            <div>
              <h1 className="text-3xl font-bold">HTTP Server Dashboard</h1>
              <p className="text-muted-foreground">Monitor your lightweight Java HTTP server</p>
            </div>
          </div>
          <div className="flex items-center space-x-3">
            <Badge variant={serverStatus === "running" ? "default" : "secondary"}>
              {serverStatus === "running" ? "Running" : "Stopped"}
            </Badge>
            <Button onClick={toggleServer} variant={serverStatus === "running" ? "destructive" : "default"} size="sm">
              {serverStatus === "running" ? (
                <>
                  <Square className="h-4 w-4 mr-2" />
                  Stop Server
                </>
              ) : (
                <>
                  <Play className="h-4 w-4 mr-2" />
                  Start Server
                </>
              )}
            </Button>
          </div>
        </div>

        {/* Stats Cards */}
        <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
          <Card>
            <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
              <CardTitle className="text-sm font-medium">Server Uptime</CardTitle>
              <Clock className="h-4 w-4 text-muted-foreground" />
            </CardHeader>
            <CardContent>
              <div className="text-2xl font-bold">{serverStats.uptime}</div>
            </CardContent>
          </Card>
          <Card>
            <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
              <CardTitle className="text-sm font-medium">Total Requests</CardTitle>
              <Activity className="h-4 w-4 text-muted-foreground" />
            </CardHeader>
            <CardContent>
              <div className="text-2xl font-bold">{serverStats.totalRequests.toLocaleString()}</div>
            </CardContent>
          </Card>
          <Card>
            <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
              <CardTitle className="text-sm font-medium">Active Connections</CardTitle>
              <Users className="h-4 w-4 text-muted-foreground" />
            </CardHeader>
            <CardContent>
              <div className="text-2xl font-bold">{serverStats.activeConnections}</div>
            </CardContent>
          </Card>
          <Card>
            <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
              <CardTitle className="text-sm font-medium">Memory Usage</CardTitle>
              <HardDrive className="h-4 w-4 text-muted-foreground" />
            </CardHeader>
            <CardContent>
              <div className="text-2xl font-bold">{serverStats.memoryUsage}</div>
            </CardContent>
          </Card>
        </div>

        {/* Main Content */}
        <Tabs defaultValue="logs" className="space-y-4">
          <TabsList>
            <TabsTrigger value="logs">Request Logs</TabsTrigger>
            <TabsTrigger value="routes">Routes</TabsTrigger>
            <TabsTrigger value="files">Static Files</TabsTrigger>
          </TabsList>

          <TabsContent value="logs" className="space-y-4">
            <Card>
              <CardHeader>
                <div className="flex items-center justify-between">
                  <div>
                    <CardTitle className="flex items-center space-x-2">
                      <Activity className="h-5 w-5" />
                      <span>Real-time Request Logs</span>
                    </CardTitle>
                    <CardDescription>Live monitoring of HTTP requests</CardDescription>
                  </div>
                  <Button variant="outline" size="sm">
                    <RefreshCw className="h-4 w-4 mr-2" />
                    Clear Logs
                  </Button>
                </div>
              </CardHeader>
              <CardContent>
                <ScrollArea className="h-96">
                  <div className="space-y-2">
                    {requestLogs.map((log) => (
                      <div key={log.id} className="flex items-center justify-between p-3 border rounded-lg">
                        <div className="flex items-center space-x-4">
                          <Badge variant="outline">{log.method}</Badge>
                          <span className="font-mono text-sm">{log.path}</span>
                          <span className={`font-semibold ${getStatusColor(log.status)}`}>{log.status}</span>
                        </div>
                        <div className="flex items-center space-x-4 text-sm text-muted-foreground">
                          <span>{log.responseTime}ms</span>
                          <span>{log.clientIp}</span>
                          <span>{log.timestamp}</span>
                        </div>
                      </div>
                    ))}
                  </div>
                </ScrollArea>
              </CardContent>
            </Card>
          </TabsContent>

          <TabsContent value="routes" className="space-y-4">
            <Card>
              <CardHeader>
                <CardTitle className="flex items-center space-x-2">
                  <Globe className="h-5 w-5" />
                  <span>Route Management</span>
                </CardTitle>
                <CardDescription>Configure HTTP routes and handlers</CardDescription>
              </CardHeader>
              <CardContent className="space-y-4">
                <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
                  <div className="space-y-2">
                    <Label htmlFor="route-path">Route Path</Label>
                    <Input
                      id="route-path"
                      placeholder="/api/example"
                      value={newRoute.path}
                      onChange={(e) => setNewRoute((prev) => ({ ...prev, path: e.target.value }))}
                    />
                  </div>
                  <div className="space-y-2">
                    <Label htmlFor="route-handler">Handler Class</Label>
                    <Input
                      id="route-handler"
                      placeholder="com.example.ExampleHandler"
                      value={newRoute.handler}
                      onChange={(e) => setNewRoute((prev) => ({ ...prev, handler: e.target.value }))}
                    />
                  </div>
                  <div className="flex items-end">
                    <Button onClick={addRoute} className="w-full">
                      Add Route
                    </Button>
                  </div>
                </div>

                <Separator />

                <div className="space-y-3">
                  <h3 className="text-lg font-semibold">Configured Routes</h3>
                  {[
                    { path: "/api/users", handler: "com.example.UserHandler", method: "GET, POST" },
                    { path: "/api/data", handler: "com.example.DataHandler", method: "GET" },
                    { path: "/health", handler: "com.example.HealthHandler", method: "GET" },
                    { path: "/static/*", handler: "StaticFileHandler", method: "GET" },
                  ].map((route, index) => (
                    <div key={index} className="flex items-center justify-between p-3 border rounded-lg">
                      <div className="space-y-1">
                        <div className="font-mono text-sm font-semibold">{route.path}</div>
                        <div className="text-sm text-muted-foreground">{route.handler}</div>
                      </div>
                      <Badge variant="secondary">{route.method}</Badge>
                    </div>
                  ))}
                </div>
              </CardContent>
            </Card>
          </TabsContent>

          <TabsContent value="files" className="space-y-4">
            <Card>
              <CardHeader>
                <CardTitle className="flex items-center space-x-2">
                  <FileText className="h-5 w-5" />
                  <span>Static File Server</span>
                </CardTitle>
                <CardDescription>Manage static files and directories</CardDescription>
              </CardHeader>
              <CardContent>
                <div className="space-y-4">
                  <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                    <div className="space-y-2">
                      <Label>Document Root</Label>
                      <Input value="/var/www/html" readOnly />
                    </div>
                    <div className="space-y-2">
                      <Label>Default Index</Label>
                      <Input value="index.html" readOnly />
                    </div>
                  </div>

                  <Separator />

                  <div className="space-y-3">
                    <h3 className="text-lg font-semibold">Recent File Requests</h3>
                    <ScrollArea className="h-48">
                      {[
                        { file: "/index.html", size: "2.4 KB", requests: 45 },
                        { file: "/css/style.css", size: "15.2 KB", requests: 32 },
                        { file: "/js/app.js", size: "8.7 KB", requests: 28 },
                        { file: "/images/logo.png", size: "12.1 KB", requests: 15 },
                        { file: "/favicon.ico", size: "1.2 KB", requests: 89 },
                      ].map((file, index) => (
                        <div key={index} className="flex items-center justify-between p-3 border rounded-lg mb-2">
                          <div className="space-y-1">
                            <div className="font-mono text-sm">{file.file}</div>
                            <div className="text-sm text-muted-foreground">{file.size}</div>
                          </div>
                          <Badge variant="outline">{file.requests} requests</Badge>
                        </div>
                      ))}
                    </ScrollArea>
                  </div>
                </div>
              </CardContent>
            </Card>
          </TabsContent>
        </Tabs>
      </div>
    </div>
  )
}
