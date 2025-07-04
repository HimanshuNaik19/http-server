"use client"

import { useState, useEffect, useCallback } from "react"
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card"
import { Badge } from "@/components/ui/badge"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs"
import { ScrollArea } from "@/components/ui/scroll-area"
import { Separator } from "@/components/ui/separator"
import { Alert, AlertDescription } from "@/components/ui/alert"
import {
  Server,
  Activity,
  FileText,
  Globe,
  Play,
  Square,
  RefreshCw,
  Clock,
  Users,
  HardDrive,
  AlertCircle,
  Wifi,
  WifiOff,
} from "lucide-react"

interface RequestLog {
  id: string
  timestamp: string
  method: string
  path: string
  status: number
  responseTime: number
  clientIp: string
  userAgent?: string
}

interface ServerStats {
  uptime: string
  totalRequests: number
  activeConnections: number
  memoryUsage: string
  cpuUsage?: number
}

interface Route {
  path: string
  handler: string
  method: string
  enabled: boolean
}

interface ServerConfig {
  port: number
  documentRoot: string
  defaultIndex: string
}

export default function HttpServerDashboard() {
  const [serverUrl, setServerUrl] = useState("http://localhost:8080")
  const [connectionStatus, setConnectionStatus] = useState<"connected" | "disconnected" | "connecting">("disconnected")
  const [serverStatus, setServerStatus] = useState<"running" | "stopped" | "unknown">("unknown")
  const [requestLogs, setRequestLogs] = useState<RequestLog[]>([])
  const [serverStats, setServerStats] = useState<ServerStats>({
    uptime: "0s",
    totalRequests: 0,
    activeConnections: 0,
    memoryUsage: "0 MB",
  })
  const [routes, setRoutes] = useState<Route[]>([])
  const [serverConfig, setServerConfig] = useState<ServerConfig>({
    port: 8080,
    documentRoot: "/static",
    defaultIndex: "index.html",
  })
  const [newRoute, setNewRoute] = useState({ path: "", handler: "", method: "GET" })
  const [error, setError] = useState<string | null>(null)
  const [wsConnection, setWsConnection] = useState<WebSocket | null>(null)

  // API call helper
  const apiCall = useCallback(
    async (endpoint: string, options: RequestInit = {}) => {
      try {
        const response = await fetch(`${serverUrl}/api${endpoint}`, {
          ...options,
          headers: {
            "Content-Type": "application/json",
            ...options.headers,
          },
        })

        if (!response.ok) {
          throw new Error(`HTTP ${response.status}: ${response.statusText}`)
        }

        return await response.json()
      } catch (err) {
        console.error(`API call failed for ${endpoint}:`, err)
        throw err
      }
    },
    [serverUrl],
  )

  // Fetch server status
  const fetchServerStatus = useCallback(async () => {
    try {
      const data = await apiCall("/server/status")
      setServerStatus(data.status)
      setConnectionStatus("connected")
      setError(null)
    } catch (err) {
      setConnectionStatus("disconnected")
      setServerStatus("unknown")
      setError(err instanceof Error ? err.message : "Failed to connect to server")
    }
  }, [apiCall])

  // Fetch server statistics
  const fetchServerStats = useCallback(async () => {
    try {
      const data = await apiCall("/server/stats")
      setServerStats(data)
    } catch (err) {
      console.error("Failed to fetch server stats:", err)
    }
  }, [apiCall])

  // Fetch routes
  const fetchRoutes = useCallback(async () => {
    try {
      const data = await apiCall("/routes")
      setRoutes(data.routes || [])
    } catch (err) {
      console.error("Failed to fetch routes:", err)
    }
  }, [apiCall])

  // Fetch server configuration
  const fetchServerConfig = useCallback(async () => {
    try {
      const data = await apiCall("/server/config")
      setServerConfig(data)
    } catch (err) {
      console.error("Failed to fetch server config:", err)
    }
  }, [apiCall])

  // Fetch request logs
  const fetchRequestLogs = useCallback(async () => {
    try {
      const data = await apiCall("/logs?limit=50")
      setRequestLogs(data.logs || [])
    } catch (err) {
      console.error("Failed to fetch request logs:", err)
    }
  }, [apiCall])

  // Setup WebSocket connection for real-time logs
  const setupWebSocket = useCallback(() => {
    if (wsConnection) {
      wsConnection.close()
    }

    try {
      const wsUrl = serverUrl.replace("http://", "ws://").replace("https://", "wss://") + "/ws/logs"
      const ws = new WebSocket(wsUrl)

      ws.onopen = () => {
        console.log("WebSocket connected")
        setConnectionStatus("connected")
      }

      ws.onmessage = (event) => {
        try {
          const logEntry = JSON.parse(event.data)
          setRequestLogs((prev) => [logEntry, ...prev.slice(0, 49)])

          // Update stats when new request comes in
          setServerStats((prev) => ({
            ...prev,
            totalRequests: prev.totalRequests + 1,
          }))
        } catch (err) {
          console.error("Failed to parse WebSocket message:", err)
        }
      }

      ws.onclose = () => {
        console.log("WebSocket disconnected")
        setConnectionStatus("disconnected")
      }

      ws.onerror = (error) => {
        console.error("WebSocket error:", error)
        setConnectionStatus("disconnected")
      }

      setWsConnection(ws)
    } catch (err) {
      console.error("Failed to setup WebSocket:", err)
      setConnectionStatus("disconnected")
    }
  }, [serverUrl, wsConnection])

  // Initial data fetch
  useEffect(() => {
    if (connectionStatus === "connected") {
      fetchServerStats()
      fetchRoutes()
      fetchServerConfig()
      fetchRequestLogs()
      setupWebSocket()
    }
  }, [connectionStatus, fetchServerStats, fetchRoutes, fetchServerConfig, fetchRequestLogs, setupWebSocket])

  // Periodic stats update
  useEffect(() => {
    if (connectionStatus === "connected" && serverStatus === "running") {
      const interval = setInterval(() => {
        fetchServerStats()
      }, 5000) // Update every 5 seconds

      return () => clearInterval(interval)
    }
  }, [connectionStatus, serverStatus, fetchServerStats])

  // Cleanup WebSocket on unmount
  useEffect(() => {
    return () => {
      if (wsConnection) {
        wsConnection.close()
      }
    }
  }, [wsConnection])

  // Connect to server
  const connectToServer = async () => {
    setConnectionStatus("connecting")
    await fetchServerStatus()
  }

  // Toggle server
  const toggleServer = async () => {
    try {
      const action = serverStatus === "running" ? "stop" : "start"
      await apiCall(`/server/${action}`, { method: "POST" })
      await fetchServerStatus()
    } catch (err) {
      setError(err instanceof Error ? err.message : "Failed to toggle server")
    }
  }

  // Add new route
  const addRoute = async () => {
    if (newRoute.path && newRoute.handler) {
      try {
        await apiCall("/routes", {
          method: "POST",
          body: JSON.stringify(newRoute),
        })
        setNewRoute({ path: "", handler: "", method: "GET" })
        await fetchRoutes()
      } catch (err) {
        setError(err instanceof Error ? err.message : "Failed to add route")
      }
    }
  }

  // Clear logs
  const clearLogs = async () => {
    try {
      await apiCall("/logs", { method: "DELETE" })
      setRequestLogs([])
    } catch (err) {
      setError(err instanceof Error ? err.message : "Failed to clear logs")
    }
  }

  const getStatusColor = (status: number) => {
    if (status >= 200 && status < 300) return "text-green-600"
    if (status >= 300 && status < 400) return "text-yellow-600"
    if (status >= 400 && status < 500) return "text-orange-600"
    return "text-red-600"
  }

  return (
    <div className="min-h-screen bg-background p-6">
      <div className="max-w-7xl mx-auto space-y-6">
        {/* Connection Header */}
        <Card>
          <CardHeader>
            <div className="flex items-center justify-between">
              <div className="flex items-center space-x-3">
                <Server className="h-8 w-8 text-primary" />
                <div>
                  <h1 className="text-3xl font-bold">HTTP Server Dashboard</h1>
                  <p className="text-muted-foreground">Monitor your Java HTTP server</p>
                </div>
              </div>
              <div className="flex items-center space-x-3">
                {connectionStatus === "connected" ? (
                  <Wifi className="h-5 w-5 text-green-600" />
                ) : (
                  <WifiOff className="h-5 w-5 text-red-600" />
                )}
                <Badge variant={connectionStatus === "connected" ? "default" : "secondary"}>{connectionStatus}</Badge>
              </div>
            </div>
          </CardHeader>
          <CardContent className="space-y-4">
            <div className="flex items-center space-x-4">
              <div className="flex-1">
                <Label htmlFor="server-url">Server URL</Label>
                <Input
                  id="server-url"
                  value={serverUrl}
                  onChange={(e) => setServerUrl(e.target.value)}
                  placeholder="http://localhost:8080"
                />
              </div>
              <div className="flex items-end space-x-2">
                <Button onClick={connectToServer} disabled={connectionStatus === "connecting"}>
                  {connectionStatus === "connecting" ? "Connecting..." : "Connect"}
                </Button>
                {connectionStatus === "connected" && (
                  <Button onClick={toggleServer} variant={serverStatus === "running" ? "destructive" : "default"}>
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
                )}
              </div>
            </div>

            {error && (
              <Alert variant="destructive">
                <AlertCircle className="h-4 w-4" />
                <AlertDescription>{error}</AlertDescription>
              </Alert>
            )}
          </CardContent>
        </Card>

        {connectionStatus === "connected" && (
          <>
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
                        <CardDescription>Live monitoring of HTTP requests via WebSocket</CardDescription>
                      </div>
                      <Button variant="outline" size="sm" onClick={clearLogs}>
                        <RefreshCw className="h-4 w-4 mr-2" />
                        Clear Logs
                      </Button>
                    </div>
                  </CardHeader>
                  <CardContent>
                    <ScrollArea className="h-96">
                      <div className="space-y-2">
                        {requestLogs.length === 0 ? (
                          <div className="text-center text-muted-foreground py-8">
                            No request logs available. Make some requests to your server to see them here.
                          </div>
                        ) : (
                          requestLogs.map((log) => (
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
                          ))
                        )}
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
                    <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
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
                      <div className="space-y-2">
                        <Label htmlFor="route-method">Method</Label>
                        <select
                          id="route-method"
                          className="w-full px-3 py-2 border border-input rounded-md"
                          value={newRoute.method}
                          onChange={(e) => setNewRoute((prev) => ({ ...prev, method: e.target.value }))}
                        >
                          <option value="GET">GET</option>
                          <option value="POST">POST</option>
                          <option value="PUT">PUT</option>
                          <option value="DELETE">DELETE</option>
                        </select>
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
                      {routes.length === 0 ? (
                        <div className="text-center text-muted-foreground py-4">No routes configured yet.</div>
                      ) : (
                        routes.map((route, index) => (
                          <div key={index} className="flex items-center justify-between p-3 border rounded-lg">
                            <div className="space-y-1">
                              <div className="font-mono text-sm font-semibold">{route.path}</div>
                              <div className="text-sm text-muted-foreground">{route.handler}</div>
                            </div>
                            <div className="flex items-center space-x-2">
                              <Badge variant="secondary">{route.method}</Badge>
                              <Badge variant={route.enabled ? "default" : "outline"}>
                                {route.enabled ? "Enabled" : "Disabled"}
                              </Badge>
                            </div>
                          </div>
                        ))
                      )}
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
                      <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
                        <div className="space-y-2">
                          <Label>Server Port</Label>
                          <Input value={serverConfig.port} readOnly />
                        </div>
                        <div className="space-y-2">
                          <Label>Document Root</Label>
                          <Input value={serverConfig.documentRoot} readOnly />
                        </div>
                        <div className="space-y-2">
                          <Label>Default Index</Label>
                          <Input value={serverConfig.defaultIndex} readOnly />
                        </div>
                      </div>

                      <Separator />

                      <div className="text-center text-muted-foreground py-8">
                        Static file statistics will be displayed here when available from your server.
                      </div>
                    </div>
                  </CardContent>
                </Card>
              </TabsContent>
            </Tabs>
          </>
        )}
      </div>
    </div>
  )
}
